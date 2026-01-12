from sqlalchemy import create_engine, Column, Integer, String, Boolean, Float
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv
import os

load_dotenv()

DATABASE_URL = os.getenv(
    "DATABASE_URL", "mysql+pymysql://root:devpassword@localhost:3306/myapp"
)

engine = create_engine(
    DATABASE_URL, echo=True
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()



class User(Base):
    """User table - stores user accounts"""

    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)


class Movie(Base):
    """Movie table - stores movie information"""

    __tablename__ = "movies"

    id = Column(Integer, primary_key=True, index=True, autoincrement=False)
    title = Column(String(255), nullable=False, index=True)
    release_date = Column(
            String(20), nullable=True, index=True
    year = Column(
        Integer, nullable=True, index=True
    genre = Column(String(200), nullable=True)
    rating = Column(Float, nullable=True)
    runtime = Column(Integer, nullable=True)
    poster_url = Column(String(500), nullable=True)
    backdrop_url = Column(String(500), nullable=True)
    overview = Column(String(2000), nullable=True)


class Watched(Base):
    """Watched table - tracks which users watched which movies"""

    __tablename__ = "watched"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, nullable=False, index=True)
    movie_id = Column(Integer, nullable=False, index=True)
    watched = Column(Boolean, default=True, nullable=False)
    like = Column(Boolean, default=False)
    watched_date = Column(String(50), nullable=True)




def get_db():
    """
    Dependency function that provides a database session.
    Use this with FastAPI's Depends() to get a DB session in your endpoints.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """
    Creates all tables in the database.
    Run this once when setting up your app.
    """
    Base.metadata.create_all(bind=engine)
