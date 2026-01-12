package com.example.cinematch2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cinematch2.Domain.FilmItemModel
import com.example.cinematch2.ViewModel.MainViewModel

@Preview
@Composable
fun SearchBarPreview() {
    SearchBar(hint = "Search for a movie...", viewModel = viewModel(), onItemClick = {})
}

@Composable
fun SearchBar(
    hint: String = "",
    viewModel: MainViewModel,
    onItemClick: (FilmItemModel) -> Unit
) {
    Column() {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    color = Color(0x20ffffff),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id=R.drawable.search),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(value = viewModel.searchText, onValueChange = { viewModel.onSearchTextChange(it) },
                placeholder = {
                    Text(text = hint,
                        color = Color(0xffbdbdbd)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                shape = RoundedCornerShape(50.dp),
                singleLine = true
            )
        }
        if (viewModel.searchText.isNotEmpty()) {
            val results = viewModel.searchResults
            if (results.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color(0xFF2B2B2B), RoundedCornerShape(8.dp))
                ) {
                    results.forEach { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            AsyncImage(
                                model = movie.posterPath,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(72.dp)
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 8.dp),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = movie.title,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onSearchTextChange("")
                                        onItemClick(movie)
                                    }
                                    .padding(16.dp)
                                    .align(Alignment.CenterVertically),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}