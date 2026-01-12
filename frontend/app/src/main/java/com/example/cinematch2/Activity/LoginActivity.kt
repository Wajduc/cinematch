package com.example.cinematch2.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.cinematch2.R
import com.example.cinematch2.Repository.AuthRepository
import com.example.cinematch2.ViewModel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        val sharedPref = getSharedPreferences("CineMatchPrefs", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("token", "") ?: ""

        if (savedToken.isNotEmpty()) {
            // User is already logged in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            LoginScreen(onLoginClick = { username, password ->
                lifecycleScope.launch {
                    val token = authRepository.login(username, password)
                    if (token != null) {
                        val sharedPref = getSharedPreferences("CineMatchPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("token", token).apply()

                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview(){
    LoginScreen(onLoginClick = { u, p -> })
}

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit){
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = colorResource(R.color.blackBackground))
    ) {
        Image(
            painter = painterResource(id=R.drawable.bg1),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(128.dp))
            Text(text="Log in",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(128.dp))
            var userText by remember { mutableStateOf("") }
            GradientTextField(
                value = userText,
                onValueChange = { userText = it },
                hint = "Username",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            var passText by remember { mutableStateOf("") }
            GradientTextField(
                value = passText,
                onValueChange = { passText = it },
                hint = "Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Don't have an account? Sign Up",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable {
                        context.startActivity(Intent(context, SignUpActivity::class.java))
                    }
            )
            Spacer(modifier = Modifier.height(64.dp))
            GradientButton(
                text = "Login",
                onClick = {
                    if (userText.isNotEmpty() && passText.isNotEmpty()) {
                        onLoginClick(userText, passText)
                    } else {
                        Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }
}

@Composable
fun GradientButton(
    text:String,
    onClick:() -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick,
        modifier=modifier,
        shape = RoundedCornerShape(60.dp),
        border = BorderStroke(
            width = 4.dp, brush = Brush.linearGradient(
                colors = listOf(colorResource(R.color.pink), colorResource(R.color.green))
            )
        ),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GradientTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    hint: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    Box(modifier = modifier
        .height(60.dp)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(colorResource(R.color.pink), colorResource(R.color.green))
            ),
            shape = RoundedCornerShape(50.dp)
        )
        .padding(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = visualTransformation,
            placeholder = {
                Text(text = hint,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White,
                unfocusedPlaceholderColor = Color.White
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.black1),
                    shape = RoundedCornerShape(50.dp)
                )
                .align(Alignment.Center),
            enabled = enabled
        )
    }
}