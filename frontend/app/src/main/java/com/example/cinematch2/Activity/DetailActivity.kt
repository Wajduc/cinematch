package com.example.cinematch2.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.cinematch2.Domain.FilmItemModel
import com.example.cinematch2.R
import com.example.cinematch2.Repository.MainRepository
import com.example.cinematch2.ViewModel.MainViewModel
import kotlinx.coroutines.launch

class DetailActivity : BaseActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentFilm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("object", FilmItemModel::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            (intent.getSerializableExtra("object") as FilmItemModel)
        }

        val fullFilmState = mutableStateOf(intentFilm)
        val isLoading = mutableStateOf(true)

        val sharedPref = getSharedPreferences("CineMatchPrefs", MODE_PRIVATE)
        val savedToken = sharedPref.getString("token", "") ?: ""

        lifecycleScope.launch {
            val repo = MainRepository()
            val fullDetails = repo.fetchMovieDetails(intentFilm.id)

            if (fullDetails != null && savedToken.isNotEmpty()) {
                val statusResponse = repo.checkSpecificStatus(intentFilm.id, savedToken)

                fullDetails.watched = statusResponse?.watched ?: intentFilm.watched
                fullDetails.like = statusResponse?.like ?: intentFilm.like

                fullFilmState.value = fullDetails
            } else if (fullDetails != null) {
                fullFilmState.value = fullDetails
            }

            isLoading.value = false
        }

        setContent {
            val film by fullFilmState
            DetailScreen(
                film = film,
                isLoading = isLoading.value,
                onBackClick = { finish() },
                onFavClick = {
                    if (savedToken.isNotEmpty()) {
                        lifecycleScope.launch {
                            val repo = MainRepository()
                            val currentLikeStatus = film.like
                            val newLikeStatus = !currentLikeStatus

                            // If liking a movie, also mark it as watched
                            val shouldMarkWatched = newLikeStatus && !film.watched

                            val likeSuccess = repo.toggleLikeStatus(
                                token = savedToken,
                                movieId = film.id,
                                currentlyLiked = currentLikeStatus
                            )

                            var watchSuccess = true
                            if (shouldMarkWatched && likeSuccess) {
                                watchSuccess = repo.toggleWatchStatus(
                                    token = savedToken,
                                    movieId = film.id,
                                    isMarkingAsWatched = true
                                )
                            }

                            if (likeSuccess && watchSuccess) {
                                film.like = newLikeStatus
                                if (shouldMarkWatched) {
                                    film.watched = true
                                }
                                fullFilmState.value = film.copy(
                                    like = newLikeStatus,
                                    watched = if (shouldMarkWatched) true else film.watched
                                )

                                mainViewModel.updateMovieLikeState(film)

                                Toast.makeText(
                                    this@DetailActivity,
                                    if (newLikeStatus) "Added to favorites and marked as watched" else "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetailActivity,
                                    "Failed to update favorite status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@DetailActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    }
                },
                onWatchedClick = {
                    if (savedToken.isNotEmpty()) {
                        lifecycleScope.launch {
                            val repo = MainRepository()
                            val currentWatchedStatus = film.watched
                            val newWatchedStatus = !currentWatchedStatus

                            // Don't allow unwatching if movie is liked
                            if (!newWatchedStatus && film.like) {
                                Toast.makeText(
                                    this@DetailActivity,
                                    "Cannot unwatch a favorited movie. Remove from favorites first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            val success = repo.toggleWatchStatus(
                                token = savedToken,
                                movieId = film.id,
                                isMarkingAsWatched = newWatchedStatus
                            )

                            if (success) {
                                film.watched = newWatchedStatus
                                fullFilmState.value = film.copy(watched = newWatchedStatus)

                                mainViewModel.updateMovieWatchState(film)

                                Toast.makeText(
                                    this@DetailActivity,
                                    if (newWatchedStatus) "Marked as watched" else "Unmarked as watched",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetailActivity,
                                    "Failed to update watched status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@DetailActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
@Preview
fun DetailScreenPreview() {
    val dummyFilm = FilmItemModel(
        title = "Bad Boys",
        description = "Description here",
        rating = 7.0,
        year = 2024,
    )
    DetailScreen(film = dummyFilm, onBackClick = {}, onFavClick = {}, isLoading = false, onWatchedClick = {})
}

@Composable
fun DetailScreen(
    film: FilmItemModel,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onFavClick: () -> Unit,
    onWatchedClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isFavorite by remember(film.id, film.like) { mutableStateOf(film.like) }
    var isWatched by remember(film.id, film.watched) { mutableStateOf(film.watched) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.blackBackground))
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier.height(400.dp)
                ) {
                    Image(
                        contentDescription = "",
                        painter = painterResource(R.drawable.back),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 48.dp)
                            .clickable { onBackClick() }
                    )
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp, top = 48.dp)
                            .align(Alignment.TopEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(color = Color(0x20ffffff))
                                .clickable {
                                    // Don't allow unwatching if favorited
                                    if (!(!isWatched && isFavorite)) {
                                        isWatched = !isWatched
                                        onWatchedClick()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "Watched",
                                tint = if (isWatched) Color.Green else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Image(
                            contentDescription = "",
                            painter = painterResource(
                                if (isFavorite) R.drawable.fav_red else R.drawable.fav
                            ),
                            modifier = Modifier.clickable {
                                isFavorite = !isFavorite
                                // If favoriting, also mark as watched
                                if (isFavorite) {
                                    isWatched = true
                                }
                                onFavClick()
                            }
                        )
                    }
                    AsyncImage(
                        film.posterPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.1f
                    )
                    AsyncImage(
                        model = film.posterPath,
                        contentDescription = null,
                        modifier = Modifier
                            .size(210.dp, 300.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .align(Alignment.BottomCenter),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        colorResource(R.color.black2),
                                        colorResource(R.color.black1)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                    Text(
                        text = film.title,
                        style = TextStyle(color = Color.White, fontSize = 27.sp),
                        modifier = Modifier
                            .padding(end = 16.dp, top = 48.dp)
                            .align(Alignment.BottomCenter)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.star),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "IMDB: ${film.rating}", color = Color.White)

                        Icon(
                            painter = painterResource(R.drawable.time),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "Runtime: ${film.runtime} min", color = Color.White)

                        Icon(
                            painter = painterResource(R.drawable.cal),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "Release: ${film.year}", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Summary", style = TextStyle(color = Color.White, fontSize = 16.sp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (isWatched) "WATCHED" else "NOT WATCHED",
                            color = if (isWatched) Color.Green else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                // Don't allow unwatching if favorited
                                if (!(!isWatched && isFavorite)) {
                                    isWatched = !isWatched
                                    onWatchedClick()
                                }
                            }
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        film.description,
                        style = TextStyle(color = Color.White, fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Actors",
                        style = TextStyle(color = Color.White, fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}