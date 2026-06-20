package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoBlue,
    secondary = BentoMint,
    tertiary = BentoRose,
    background = BentoSlateDark,
    surface = Color(0xFF1E293B), // slate-800
    onPrimary = BentoSlateDark,
    onSecondary = BentoSlateDark,
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9), // slate-100
    onSurface = Color(0xFFF1F5F9)     // slate-100
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoSlateDark,
    secondary = BentoBlue,
    tertiary = BentoMint,
    background = BentoBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = BentoSlateDark,
    onTertiary = Color(0xFF0F172A),
    onBackground = BentoSlateMedium,
    onSurface = BentoSlateMedium
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic system color by default to strictly preserve our gorgeous brand colors (#A7D8F0, #B7E4C7)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
