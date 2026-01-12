import pandas as pd
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy.orm import Session
from database import Movie, Watched


def get_user_recommendations(db: Session, user_id: int, top_n: int = 10):
    """
    Generate movie recommendations using content-based filtering with cosine similarity.

    Steps:
    1. Get all movies and convert genres to binary vectors
    2. Get user's liked movies and create user profile (average of liked movies)
    3. Calculate cosine similarity between user profile and all unwatched movies
    4. Return top N most similar movies
    """

    all_movies = db.query(Movie).all()

    if not all_movies:
        return []

    all_genres = set()
    for movie in all_movies:
        if movie.genre:
            genres = [g.strip() for g in movie.genre.split(",")]
            all_genres.update(genres)

    all_genres = sorted(list(all_genres))

    movie_vectors = []
    movie_ids = []

    for movie in all_movies:
        vector = [0] * len(all_genres)
        if movie.genre:
            movie_genres = [g.strip() for g in movie.genre.split(",")]
            for i, genre in enumerate(all_genres):
                if genre in movie_genres:
                    vector[i] = 1
        movie_vectors.append(vector)
        movie_ids.append(movie.id)

    movie_matrix = np.array(movie_vectors)

    liked_movies = (
        db.query(Watched).filter(Watched.user_id == user_id, Watched.like == True).all()
    )

    if not liked_movies:
        top_rated = db.query(Movie).order_by(Movie.rating.desc()).limit(top_n).all()
        return top_rated

    liked_movie_ids = [w.movie_id for w in liked_movies]
    liked_indices = [
        movie_ids.index(mid) for mid in liked_movie_ids if mid in movie_ids
    ]

    if not liked_indices:
        top_rated = db.query(Movie).order_by(Movie.rating.desc()).limit(top_n).all()
        return top_rated

    user_profile = movie_matrix[liked_indices].mean(axis=0).reshape(1, -1)

    watched_movie_ids = (
        db.query(Watched.movie_id).filter(Watched.user_id == user_id).all()
    )
    watched_movie_ids = [w[0] for w in watched_movie_ids]

    similarities = cosine_similarity(user_profile, movie_matrix)[0]

    recommendations = []
    for i, movie_id in enumerate(movie_ids):
        if movie_id not in watched_movie_ids:
            recommendations.append((movie_id, similarities[i]))

    recommendations.sort(key=lambda x: x[1], reverse=True)
    top_recommendations = recommendations[:top_n]

    recommended_movie_ids = [rec[0] for rec in top_recommendations]
    recommended_movies = (
        db.query(Movie).filter(Movie.id.in_(recommended_movie_ids)).all()
    )

    movie_dict = {m.id: m for m in recommended_movies}
    sorted_movies = [
        movie_dict[mid] for mid in recommended_movie_ids if mid in movie_dict
    ]

    return sorted_movies
