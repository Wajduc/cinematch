from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import os

load_dotenv()

from database import init_db, SessionLocal
from seed_movies import seed_movies

import auth
import movies

app = FastAPI(
    title="CineMatch API",
    description="Movie tracking and recommendation API",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(movies.router)


@app.on_event("startup")
def on_startup():
    # Create database tables
    init_db()
    print("✓ Database tables created!")

    tmdb_api_key = os.getenv("TMDB_API_KEY")
    if tmdb_api_key:
        db = SessionLocal()
        try:
            seed_movies(
                db,
                tmdb_api_key,
                num_top_rated=100,
                num_recent=50,
                num_upcoming=30,
            )
        finally:
            db.close()
    else:
        print("⚠ TMDB_API_KEY not found - skipping movie seeding")


@app.get("/")
def root():
    return {"message": "Welcome to CineMatch API", "version": "1.0.0", "docs": "/docs"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}
