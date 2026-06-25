package com.example.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ForestBackground
import com.example.ui.components.ForestState
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(gameStateCurrent(authState)) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    // Adapt to true dynamic clock time to match the rest of the application's day/night rules
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val isDay = hour in 6..17

    val themeColors = if (isDay) {
        com.example.ui.theme.AppThemeColors(
            surface = Color(0xFFFFFFFF).copy(alpha = 0.82f),
            onSurface = Color(0xFF1A1A2E),
            secondaryText = Color(0xFF57606A),
            accent = Color(0xFF7C6AF7),
            inputBackground = Color(0xFFF0F2F5).copy(alpha = 0.80f),
            divider = Color(0xFFD0D7DE),
            iconTint = Color(0xFF1A1A2E)
        )
    } else {
        com.example.ui.theme.AppThemeColors(
            surface = Color(0xFF1C2128).copy(alpha = 0.82f),
            onSurface = Color(0xFFE6EDF3),
            secondaryText = Color(0xFF8B949E),
            accent = Color(0xFF7C6AF7),
            inputBackground = Color(0xFF0D1117).copy(alpha = 0.75f),
            divider = Color(0xFF30363D),
            iconTint = Color(0xFFCDD9E5)
        )
    }

    CompositionLocalProvider(
        com.example.ui.theme.LocalAppThemeColors provides themeColors,
        com.example.ui.theme.LocalIsDarkTheme provides !isDay
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ForestBackground(
                forestState = ForestState(treeCount = 5, isDayTime = isDay),
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FOCUS ISLAND",
                    style = MaterialTheme.typography.headlineLarge,
                    color = themeColors.onSurface,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Streamlined tactile productivity",
                    color = themeColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (isLogin) "Sign In to Your Forest 🌱" else "Plant Your First Tree 🌲",
                            style = MaterialTheme.typography.headlineSmall,
                            color = themeColors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLogin) {
                            GlassTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "Username",
                                icon = Icons.Default.Person
                            )
                        }
                        GlassTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email Address",
                            icon = Icons.Default.Email
                        )
                        GlassTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            icon = Icons.Default.Lock,
                            isPassword = true,
                            showPassword = showPass,
                            onTogglePassword = { showPass = !showPass }
                        )
                        if (isLogin) {
                            TextButton(
                                onClick = { viewModel.resetPassword(email) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Forgot Password?", color = themeColors.secondaryText)
                            }
                        }
                        if (authState is AuthState.Error) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = Color(0xFFFF6584),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = Color(0xFF7C6AF7),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            GlassButton(
                                label = if (isLogin) "Login" else "Create Account",
                                icon = Icons.Default.ArrowForward,
                                onClick = {
                                    if (isLogin) {
                                        viewModel.signIn(email, password)
                                    } else {
                                        viewModel.signUp(email, password, username)
                                    }
                                },
                                accentColor = Color(0xFF7C6AF7),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        TextButton(
                            onClick = { isLogin = !isLogin },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login",
                                color = themeColors.secondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val themeColors = com.example.ui.theme.LocalAppThemeColors.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = themeColors.secondaryText.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = themeColors.secondaryText.copy(alpha = 0.8f)) },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = themeColors.secondaryText.copy(alpha = 0.8f)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = themeColors.accent,
            unfocusedBorderColor = themeColors.divider,
            focusedTextColor = themeColors.onSurface,
            unfocusedTextColor = themeColors.onSurface,
            focusedContainerColor = themeColors.inputBackground,
            unfocusedContainerColor = themeColors.inputBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun gameStateCurrent(state: AuthState): String {
    return when (state) {
        is AuthState.Loading -> "Loading"
        is AuthState.Unauthenticated -> "Unauthenticated"
        is AuthState.Authenticated -> "Authenticated"
        is AuthState.Error -> "Error"
    }
}
