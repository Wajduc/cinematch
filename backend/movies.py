from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import func, desc, and_
from typing import List, Optional
from datetime import datetime, timedelta
import os

from database import get_db, Movie, Watched, User as DBUser
from auth import get_current_user
from recommender import get_user_recommendations
from seed_movies import search_tmdb_movies, add_tmdb_movie_to_db

router = APIRouter(prefix="/movies", tags=["Movies"])


class MarkWatchedRequest(BaseModel):
    movie_id: int
    watched: bool = True
    rating: Optional[int] = None

    class Config:
        json_schema_extra = {"example": {"movie_id": 1, "watched": True, "rating": 8}}


class WatchedResponse(BaseModel):
    id: int
    user_id: int
    movie_id: int
    watched: bool
    like: Optional[bool]

    class Config:
        from_attributes = True


class MovieListItem(BaseModel):
    """Minimal movie info for lists - only what's needed to display cards"""

    id: int
    title: str
    rating: Optional[float]
    poster_url: Optional[str]

    class Config:
        from_attributes = True


class MovieDetail(BaseModel):
    """Full movie details - returned when user clicks on a specific movie"""

    id: int
    title: str
    release_date: Optional[str]
    year: Optional[int]
    genre: Optional[str]
    rating: Optional[float]
    runtime: Optional[int]
    poster_url: Optional[str]
    backdrop_url: Optional[str]
    overview: Optional[str]

    class Config:
        from_attributes = True


class PaginatedMoviesResponse(BaseModel):
    page: int
    page_size: int
    total_pages: int
    total_movies: int
    movies: List[MovieListItem]


def get_user_favorite_genre(db: Session, user_id: int) -> Optional[str]:
    """
    Find user's most watched genre by counting watched movies per genre.
    Returns the genre name or None if user hasn't watched anything.
    """
    watched_movies = (
        db.query(Movie)
        .join(Watched, Movie.id == Watched.movie_id)
        .filter(Watched.user_id == user_id)
        .all()
    )

    if not watched_movies:
        return None

    genre_count = {}
    for movie in watched_movies:
        if movie.genre:
            genres = [g.strip() for g in movie.genre.split(",")]
            for genre in genres:
                genre_count[genre] = genre_count.get(genre, 0) + 1

    if not genre_count:
        return None

    return max(genre_count, key=genre_count.get)


def paginate_query(query, page: int, page_size: int):
    """
    Paginate a SQLAlchemy query.
    Returns (items, total_count)
    """
    total_count = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()
    return items, total_count


# ===== ENDPOINTS =====


@router.get("/latest", response_model=PaginatedMoviesResponse)
def get_latest_movies(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    Get recently released movies (released in last 90 days).
    Sorted by release date (newest first).
    """
    from datetime import datetime, timedelta

    cutoff_date = (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d")
    today = datetime.now().strftime("%Y-%m-%d")

    query = (
        db.query(Movie)
        .filter(
            and_(
                Movie.release_date <= today,  # Already released
                Movie.release_date >= cutoff_date,  # Within last 90 days
            )
        )
        .order_by(desc(Movie.release_date))
    )

    movies, total = paginate_query(query, page, page_size)
    total_pages = (total + page_size - 1) // page_size

    return {
        "page": page,
        "page_size": page_size,
        "total_pages": total_pages,
        "total_movies": total,
        "movies": movies,
    }


@router.get("/upcoming", response_model=PaginatedMoviesResponse)
def get_upcoming_movies(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    Get upcoming movie releases (not yet released).
    Sorted by release date (soonest first).
    """
    from datetime import datetime

    today = datetime.now().strftime("%Y-%m-%d")

    query = (
        db.query(Movie).filter(Movie.release_date > today).order_by(Movie.release_date)
    )

    movies, total = paginate_query(query, page, page_size)
    total_pages = (total + page_size - 1) // page_size

    return {
        "page": page,
        "page_size": page_size,
        "total_pages": total_pages,
        "total_movies": total,
        "movies": movies,
    }


@router.get("/recommended", response_model=PaginatedMoviesResponse)
def get_recommended_movies(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Get personalized movie recommendations using content-based filtering.
    Based on genres of movies the user has liked.
    """
    total_to_generate = page * page_size + 50

    all_recommendations = get_user_recommendations(
        db, current_user.id, top_n=total_to_generate
    )

    start_idx = (page - 1) * page_size
    end_idx = start_idx + page_size

    paginated_movies = all_recommendations[start_idx:end_idx]
    total = len(all_recommendations)
    total_pages = (total + page_size - 1) // page_size

    return {
        "page": page,
        "page_size": page_size,
        "total_pages": total_pages,
        "total_movies": total,
        "movies": paginated_movies,
    }


@router.get("/search", response_model=PaginatedMoviesResponse)
def search_movies(
    q: str = Query(..., min_length=1, description="Search query"),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    Search movies by title.
    Searches local database first, then falls back to TMDB API.
    TMDB results are automatically added to local database.
    """
    local_query = (
        db.query(Movie).filter(Movie.title.like(f"%{q}%")).order_by(desc(Movie.rating))
    )

    local_movies, local_total = paginate_query(local_query, page, page_size)

    if local_movies and len(local_movies) >= page_size:
        total_pages = (local_total + page_size - 1) // page_size
        return {
            "page": page,
            "page_size": page_size,
            "total_pages": total_pages,
            "total_movies": local_total,
            "movies": local_movies,
        }

    if page == 1:
        tmdb_api_key = os.getenv("TMDB_API_KEY")
        if not tmdb_api_key:
            total_pages = (
                (local_total + page_size - 1) // page_size if local_total > 0 else 1
            )
            return {
                "page": page,
                "page_size": page_size,
                "total_pages": total_pages,
                "total_movies": local_total,
                "movies": local_movies,
            }

        print(f"Searching TMDB for: {q}")
        tmdb_results = search_tmdb_movies(q, tmdb_api_key)

        tmdb_movies = []
        for tmdb_movie in tmdb_results[:page_size]:
            try:
                movie = add_tmdb_movie_to_db(db, tmdb_movie, tmdb_api_key)
                tmdb_movies.append(movie)
            except Exception as e:
                print(f"Error adding TMDB movie {tmdb_movie.get('title')}: {e}")
                continue

        print(f"Added {len(tmdb_movies)} movies from TMDB")

        combined_movies = local_movies + tmdb_movies

        seen_ids = set()
        unique_movies = []
        for movie in combined_movies:
            if movie.id not in seen_ids:
                seen_ids.add(movie.id)
                unique_movies.append(movie)

        unique_movies = unique_movies[:page_size]

        return {
            "page": page,
            "page_size": page_size,
            "total_pages": 1,
            "total_movies": len(unique_movies),
            "movies": unique_movies,
        }
    else:
        total_pages = (
            (local_total + page_size - 1) // page_size if local_total > 0 else 1
        )
        return {
            "page": page,
            "page_size": page_size,
            "total_pages": total_pages,
            "total_movies": local_total,
            "movies": local_movies,
        }


@router.get("/{movie_id}", response_model=MovieDetail)
def get_movie_by_id(movie_id: int, db: Session = Depends(get_db)):
    """
    Get a specific movie by ID.
    """
    movie = db.query(Movie).filter(Movie.id == movie_id).first()

    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")

    return movie


@router.post("/watch/{movie_id}", response_model=WatchedResponse)
def mark_movie_watched(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Mark a movie as watched.
    Creates watched entry if it doesn't exist.
    """
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")

    existing = (
        db.query(Watched)
        .filter(and_(Watched.user_id == current_user.id, Watched.movie_id == movie_id))
        .first()
    )

    if existing:
        existing.watched = True
        existing.watched_date = datetime.now().isoformat()
        db.commit()
        db.refresh(existing)
        return existing
    else:
        watched = Watched(
            user_id=current_user.id,
            movie_id=movie_id,
            watched=True,
            like=None,
            watched_date=datetime.now().isoformat(),
        )
        db.add(watched)
        db.commit()
        db.refresh(watched)
        return watched


@router.post("/like/{movie_id}", response_model=WatchedResponse)
def like_movie(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Mark a movie as liked.
    Creates watched entry if it doesn't exist.
    """
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")

    existing = (
        db.query(Watched)
        .filter(and_(Watched.user_id == current_user.id, Watched.movie_id == movie_id))
        .first()
    )

    if existing:
        existing.like = True
        existing.watched = True
        existing.watched_date = datetime.now().isoformat()
        db.commit()
        db.refresh(existing)
        return existing
    else:
        watched = Watched(
            user_id=current_user.id,
            movie_id=movie_id,
            watched=True,
            like=True,
            watched_date=datetime.now().isoformat(),
        )
        db.add(watched)
        db.commit()
        db.refresh(watched)
        return watched


@router.delete("/watch/{movie_id}")
def unmark_movie_watched(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Remove a movie from watched list.
    """
    watched = (
        db.query(Watched)
        .filter(and_(Watched.user_id == current_user.id, Watched.movie_id == movie_id))
        .first()
    )

    if not watched:
        raise HTTPException(status_code=404, detail="Movie not in watched list")

    db.delete(watched)
    db.commit()

    return {"message": "Movie removed from watched list"}


@router.delete("/like/{movie_id}", response_model=WatchedResponse)
def unlike_movie(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Remove like from a movie (sets like to False/None).
    """
    watched = (
        db.query(Watched)
        .filter(and_(Watched.user_id == current_user.id, Watched.movie_id == movie_id))
        .first()
    )

    if not watched:
        raise HTTPException(status_code=404, detail="Movie not in watched list")

    watched.like = False
    db.commit()
    db.refresh(watched)
    return watched


@router.get("/watch/me", response_model=PaginatedMoviesResponse)
def get_my_watched_movies(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Get all movies the current user has watched (paginated).
    """
    query = (
        db.query(Movie)
        .join(Watched, Movie.id == Watched.movie_id)
        .filter(and_(Watched.user_id == current_user.id, Watched.watched == True))
        .order_by(desc(Watched.watched_date))
    )

    movies, total = paginate_query(query, page, page_size)
    total_pages = (total + page_size - 1) // page_size

    return {
        "page": page,
        "page_size": page_size,
        "total_pages": total_pages,
        "total_movies": total,
        "movies": movies,
    }


@router.get("/like/me", response_model=PaginatedMoviesResponse)
def get_my_liked_movies(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Get all movies the current user has liked.
    """
    query = (
        db.query(Movie)
        .join(Watched, Movie.id == Watched.movie_id)
        .filter(and_(Watched.user_id == current_user.id, Watched.like == True))
        .order_by(desc(Watched.watched_date))
    )

    movies, total = paginate_query(query, page, page_size)
    total_pages = (total + page_size - 1) // page_size

    return {
        "page": page,
        "page_size": page_size,
        "total_pages": total_pages,
        "total_movies": total,
        "movies": movies,
    }


@router.get("/watch/{movie_id}", response_model=WatchedResponse)
def check_if_watched(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Check if current user has watched this movie.
    Returns 404 if not watched.
    """
    watched = (
        db.query(Watched)
        .filter(and_(Watched.user_id == current_user.id, Watched.movie_id == movie_id))
        .first()
    )

    if not watched:
        raise HTTPException(status_code=404, detail="Movie not watched")

    return watched


@router.get("/like/{movie_id}", response_model=WatchedResponse)
def check_if_liked(
    movie_id: int,
    current_user: DBUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Check if current user has liked this movie.
    Returns 404 if not liked (or not watched).
    """
    watched = (
        db.query(Watched)
        .filter(
            and_(
                Watched.user_id == current_user.id,
                Watched.movie_id == movie_id,
                Watched.like == True,  # Only return if actually liked
            )
        )
        .first()
    )

    if not watched:
        raise HTTPException(status_code=404, detail="Movie not liked")

    return watched
