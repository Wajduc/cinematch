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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.cinematch2.R
import com.example.cinematch2.Repository.AuthRepository
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignUpScreen(onSignUpClick = { username, email, password, repassword ->
                if (username.isNotEmpty() && password.isNotEmpty() && repassword.isNotEmpty()) {
                    if (password == repassword) {
                        lifecycleScope.launch {
                            val success = authRepository.signUp(username, email, password)
                            if (success) {
                                Toast.makeText(this@SignUpActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                            } else {
                                Toast.makeText(this@SignUpActivity, "Signup Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@SignUpActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SignUpActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

@Composable
@Preview
fun SignUpScreenPreview(){
    SignUpScreen(onSignUpClick = { u, e, p, cp -> })
}

@Composable
fun SignUpScreen(onSignUpClick: (String, String, String, String) -> Unit){
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
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(128.dp))
            Text(text="Sign up",
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
            var emailText by remember { mutableStateOf("") }
            GradientTextField(
                value = emailText,
                onValueChange = { emailText = it },
                hint = "e-mail",
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
            var repassText by remember { mutableStateOf("") }
            GradientTextField(
                value = repassText,
                onValueChange = { repassText = it },
                hint = "Confirm Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(64.dp))
            GradientButton(
                text = "Sign Up",
                onClick = {
                    if (userText.isNotEmpty() && passText.isNotEmpty() && repassText.isNotEmpty() && emailText.isNotEmpty()) {
                        onSignUpClick(userText, emailText, passText, repassText)
                    } else {
                        Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}