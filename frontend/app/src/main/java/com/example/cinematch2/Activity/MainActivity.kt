package com.example.cinematch2.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinematch2.BottomNavigationBar
import com.example.cinematch2.Domain.FilmItemModel
import com.example.cinematch2.FilmItem
import com.example.cinematch2.R
import com.example.cinematch2.SearchBar
import com.example.cinematch2.ViewModel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("CineMatchPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        Log.d("MainActivity", "onCreate - Token exists: ${token.isNotEmpty()}")

        setContent {
            LaunchedEffect(Unit) {
                if (token.isNotEmpty()) {
                    Log.d("MainActivity", "Loading initial data...")
                    viewModel.loadInitialData(token)
                }
            }

            MainScreen(viewModel = viewModel, onItemClick = { item ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("object", item)
                startActivity(intent)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("CineMatchPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        Log.d("MainActivity", "onResume - Token exists: ${token.isNotEmpty()}")

        if (token.isNotEmpty()) {
            Log.d("MainActivity", "Refreshing data...")
            viewModel.loadInitialData(token)
        }
    }
}

@Composable
@Preview
fun MainScreenPreview() {
    MainScreen(viewModel = viewModel())
}

@Composable
fun MainScreen(viewModel: MainViewModel, onItemClick: (FilmItemModel) -> Unit={}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        bottomBar = { BottomNavigationBar(
            currentScreen = "Home",
            onHomeClick = {},
            onSettingsClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        ) },
        containerColor = colorResource(R.color.blackBackground),
    ) {
            paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .background(color = colorResource(R.color.blackBackground))
        ) {
            Image(
                painter = painterResource(id=R.drawable.bg1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            MainContent(viewModel = viewModel, onItemClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MainViewModel, onItemClick: (FilmItemModel) -> Unit) {
    val latestMovies by viewModel.latestMovies.observeAsState(emptyList())
    val upcomingMovies by viewModel.upcomingMovies.observeAsState(emptyList())
    val recommendedMovies by viewModel.recommendedMovies.observeAsState(emptyList())
    val watchedMovies by viewModel.watchedMovies.observeAsState(emptyList())

    Log.d("MainContent", "Watched movies count: ${watchedMovies.size}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, bottom = 100.dp)
    ) {
        Text(
            text="What would you like to watch?",
            style = TextStyle(color = Color.White, fontSize = 25.sp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
        )

        SearchBar(
            hint = "Search for a movie...",
            viewModel = viewModel,
            onItemClick = onItemClick
        )

        if (viewModel.searchText.isEmpty()) {
            // Latest Filmi
            if (latestMovies.isNotEmpty()) {
                SectionTitle(title = "Latest Movies")
                LazyRow { items(latestMovies) { item -> FilmItem(item, onItemClick) } }
            }

            // Upcoming filmi
            if (upcomingMovies.isNotEmpty()) {
                SectionTitle(title = "Upcoming Movies")
                LazyRow { items(upcomingMovies) { item -> FilmItem(item, onItemClick) } }
            }

            // Priporočeni filmi
            if (recommendedMovies.isNotEmpty()) {
                SectionTitle(title = "Recommended For You")
                LazyRow { items(recommendedMovies) { item -> FilmItem(item, onItemClick) } }
            }

            // Ze pogledani filmi
            SectionTitle(title = "Watched Movies")
            if (watchedMovies.isNotEmpty()) {
                LazyRow { items(watchedMovies) { item -> FilmItem(item, onItemClick) } }
            } else {
                Text(
                    text = "You haven't watched any movies yet.",
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String){
    Text(
        text = title,
        style = TextStyle(color = Color(0xffffc107), fontSize = 18.sp),
        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}