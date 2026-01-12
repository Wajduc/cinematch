import requests
from sqlalchemy.orm import Session
from database import Movie
import os
from dotenv import load_dotenv

load_dotenv()

TMDB_API_KEY = os.getenv("TMDB_API_KEY")
TMDB_BASE_URL = "https://api.themoviedb.org/3"
TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p"


def fetch_movie_details(tmdb_id: int, api_key: str) -> dict:
    """
    Fetch detailed movie information including runtime.
    """
    try:
        url = f"{TMDB_BASE_URL}/movie/{tmdb_id}"
        params = {"api_key": api_key}
        response = requests.get(url, params=params)

        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"  Error fetching details for movie {tmdb_id}: {e}")

    return None


def fetch_top_rated_movies(api_key: str, total_movies: int = 100) -> list:
    """
    Fetch top rated movies from TMDB.
    TMDB returns 20 movies per page, so we'll fetch multiple pages.
    """
    movies = []
    pages_needed = (total_movies // 20) + 1

    print(f"Fetching top {total_movies} rated movies from TMDB...")

    for page in range(1, pages_needed + 1):
        try:
            url = f"{TMDB_BASE_URL}/movie/top_rated"
            params = {"api_key": api_key, "language": "en-US", "page": page}

            response = requests.get(url, params=params)

            if response.status_code == 200:
                data = response.json()
                movies.extend(data.get("results", []))
                print(
                    f"  Fetched page {page}/{pages_needed} ({len(movies)} movies so far)"
                )
            else:
                print(f"  Error fetching page {page}: {response.status_code}")
                break

        except Exception as e:
            print(f"  Error on page {page}: {e}")
            break

    return movies[:total_movies]


def fetch_now_playing_movies(api_key: str, total_movies: int = 50) -> list:
    """
    Fetch currently playing / recent movies from TMDB.
    """
    movies = []
    pages_needed = (total_movies // 20) + 1

    print(f"Fetching {total_movies} recent movies from TMDB...")

    for page in range(1, pages_needed + 1):
        try:
            url = f"{TMDB_BASE_URL}/movie/now_playing"
            params = {"api_key": api_key, "language": "en-US", "page": page}

            response = requests.get(url, params=params)

            if response.status_code == 200:
                data = response.json()
                movies.extend(data.get("results", []))
                print(
                    f"  Fetched page {page}/{pages_needed} ({len(movies)} movies so far)"
                )
            else:
                print(f"  Error fetching page {page}: {response.status_code}")
                break

        except Exception as e:
            print(f"  Error on page {page}: {e}")
            break

    return movies[:total_movies]


def fetch_upcoming_movies(api_key: str, total_movies: int = 30) -> list:
    """
    Fetch upcoming movies from TMDB.
    """
    movies = []
    pages_needed = (total_movies // 20) + 1

    print(f"Fetching {total_movies} upcoming movies from TMDB...")

    for page in range(1, pages_needed + 1):
        try:
            url = f"{TMDB_BASE_URL}/movie/upcoming"
            params = {"api_key": api_key, "language": "en-US", "page": page}

            response = requests.get(url, params=params)

            if response.status_code == 200:
                data = response.json()
                movies.extend(data.get("results", []))
                print(
                    f"  Fetched page {page}/{pages_needed} ({len(movies)} movies so far)"
                )
            else:
                print(f"  Error fetching page {page}: {response.status_code}")
                break

        except Exception as e:
            print(f"  Error on page {page}: {e}")
            break

    return movies[:total_movies]


def get_movie_genres(genre_ids: list, api_key: str = None) -> str:
    """
    Convert TMDB genre IDs to genre names.
    """
    genre_map = {
        28: "Action",
        12: "Adventure",
        16: "Animation",
        35: "Comedy",
        80: "Crime",
        99: "Documentary",
        18: "Drama",
        10751: "Family",
        14: "Fantasy",
        36: "History",
        27: "Horror",
        10402: "Music",
        9648: "Mystery",
        10749: "Romance",
        878: "Science Fiction",
        10770: "TV Movie",
        53: "Thriller",
        10752: "War",
        37: "Western",
    }

    genres = [genre_map.get(gid, "") for gid in genre_ids]
    return ", ".join([g for g in genres if g])


def search_tmdb_movies(query: str, api_key: str) -> list:
    """
    Search TMDB for movies by title.
    Returns list of movie results.
    """
    try:
        url = f"{TMDB_BASE_URL}/search/movie"
        params = {"api_key": api_key, "query": query, "language": "en-US", "page": 1}

        response = requests.get(url, params=params)

        if response.status_code == 200:
            data = response.json()
            return data.get("results", [])
    except Exception as e:
        print(f"Error searching TMDB: {e}")

    return []


def add_tmdb_movie_to_db(db: Session, tmdb_movie_data: dict, api_key: str):
    """
    Add a movie from TMDB search results to local database.
    Returns the created Movie object.
    """
    tmdb_id = tmdb_movie_data.get("id")

    existing = db.query(Movie).filter(Movie.id == tmdb_id).first()
    if existing:
        return existing

    details = fetch_movie_details(tmdb_id, api_key)
    runtime = details.get("runtime") if details else None

    release_date = tmdb_movie_data.get("release_date")
    year = None
    if release_date:
        try:
            year = int(release_date.split("-")[0])
        except:
            pass

    poster_url = None
    if tmdb_movie_data.get("poster_path"):
        poster_url = f"{TMDB_IMAGE_BASE_URL}/w500{tmdb_movie_data['poster_path']}"

    backdrop_url = None
    if tmdb_movie_data.get("backdrop_path"):
        backdrop_url = f"{TMDB_IMAGE_BASE_URL}/w1280{tmdb_movie_data['backdrop_path']}"

    movie = Movie(
        id=tmdb_id,  # Use TMDB ID
        title=tmdb_movie_data.get("title"),
        release_date=release_date,
        year=year,
        genre=get_movie_genres(tmdb_movie_data.get("genre_ids", [])),
        rating=tmdb_movie_data.get("vote_average"),
        runtime=runtime,
        poster_url=poster_url,
        backdrop_url=backdrop_url,
        overview=tmdb_movie_data.get("overview"),
    )

    db.add(movie)
    db.commit()
    db.refresh(movie)

    return movie


def seed_movies(
    db: Session,
    tmdb_api_key: str,
    num_top_rated: int = 100,
    num_recent: int = 50,
    num_upcoming: int = 30,
):
    """
    Seed the database with movies from TMDB.
    """
    if not tmdb_api_key:
        print("❌ TMDB_API_KEY not found in environment variables!")
        return

    existing_count = db.query(Movie).count()
    if existing_count > 0:
        print(f"Database already has {existing_count} movies. Skipping seed.")
        return

    print(f"Seeding database with movies from TMDB...")
    print(f"  - {num_top_rated} top rated")
    print(f"  - {num_recent} recent releases")
    print(f"  - {num_upcoming} upcoming releases")

    top_rated = fetch_top_rated_movies(tmdb_api_key, num_top_rated)
    recent = fetch_now_playing_movies(tmdb_api_key, num_recent)
    upcoming = fetch_upcoming_movies(tmdb_api_key, num_upcoming)

    all_movies = top_rated + recent + upcoming
    seen_ids = set()
    unique_movies = []
    for movie in all_movies:
        tmdb_id = movie.get("id")
        if tmdb_id not in seen_ids:
            seen_ids.add(tmdb_id)
            unique_movies.append(movie)

    if not unique_movies:
        print("❌ No movies fetched from TMDB.")
        return

    print(f"\nInserting {len(unique_movies)} unique movies into database...")

    successful = 0
    failed = 0

    for movie_data in unique_movies:
        try:
            details = fetch_movie_details(movie_data.get("id"), tmdb_api_key)
            runtime = details.get("runtime") if details else None

            release_date = movie_data.get("release_date")
            year = None
            if release_date:
                try:
                    year = int(release_date.split("-")[0])
                except:
                    pass

            poster_url = None
            if movie_data.get("poster_path"):
                poster_url = f"{TMDB_IMAGE_BASE_URL}/w500{movie_data['poster_path']}"

            backdrop_url = None
            if movie_data.get("backdrop_path"):
                backdrop_url = (
                    f"{TMDB_IMAGE_BASE_URL}/w1280{movie_data['backdrop_path']}"
                )

            movie = Movie(
                id=movie_data.get("id"),
                title=movie_data.get("title"),
                release_date=release_date,
                year=year,
                genre=get_movie_genres(movie_data.get("genre_ids", [])),
                rating=movie_data.get("vote_average"),
                runtime=runtime,
                poster_url=poster_url,
                backdrop_url=backdrop_url,
                overview=movie_data.get("overview"),
            )

            db.add(movie)
            successful += 1

            if successful % 10 == 0:
                db.commit()
                print(f"  ✓ Saved {successful}/{len(unique_movies)} movies...")

        except Exception as e:
            failed += 1
            print(f"  ✗ Error saving '{movie_data.get('title')}': {e}")

    try:
        db.commit()
        print(f"\n✓ Seeding complete! {successful} movies added, {failed} failed.")
    except Exception as e:
        print(f"❌ Error committing: {e}")
        db.rollback()
