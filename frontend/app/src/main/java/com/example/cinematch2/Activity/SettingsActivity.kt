package com.example.cinematch2.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.cinematch2.BottomNavigationBar
import com.example.cinematch2.R
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(onClick = {
                val sharedPref = getSharedPreferences("CineMatchPrefs", MODE_PRIVATE)
                sharedPref.edit { clear() }

                Toast.makeText(this@SettingsActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            })
        }
    }
}

@Composable
@Preview
fun SettingsScreenPreview() {
    SettingsScreen()
}

@Composable
fun SettingsScreen(onClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        bottomBar = { BottomNavigationBar(
            currentScreen = "Settings",
            onHomeClick = {
                context.startActivity(Intent(context, MainActivity::class.java))
            },
            onSettingsClick = {}
        ) },
        containerColor = colorResource(R.color.blackBackground),
    ) {
            paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(color = colorResource(R.color.blackBackground))
        ) {
            Image(
                painter = painterResource(id=R.drawable.bg1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(36.dp))
                Text(text = "Settings",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
                GradientButton(
                    text = "Logout",
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}