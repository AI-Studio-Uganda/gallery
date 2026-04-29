/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.ai.edge.gallery.proto.Theme

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

@Immutable
data class CustomColors(
  val appTitleGradientColors: List<Color> = listOf(),
  val tabHeaderBgColor: Color = Color.Transparent,
  val taskCardBgColor: Color = Color.Transparent,
  val taskBgColors: List<Color> = listOf(),
  val taskBgGradientColors: List<List<Color>> = listOf(),
  val taskIconColors: List<Color> = listOf(),
  val taskIconShapeBgColor: Color = Color.Transparent,
  val homeBottomGradient: List<Color> = listOf(),
  val userBubbleBgColor: Color = Color.Transparent,
  val agentBubbleBgColor: Color = Color.Transparent,
  val linkColor: Color = Color.Transparent,
  val successColor: Color = Color.Transparent,
  val recordButtonBgColor: Color = Color.Transparent,
  val waveFormBgColor: Color = Color.Transparent,
  val modelInfoIconColor: Color = Color.Transparent,
  val warningContainerColor: Color = Color.Transparent,
  val warningTextColor: Color = Color.Transparent,
  val errorContainerColor: Color = Color.Transparent,
  val errorTextColor: Color = Color.Transparent,
  val newFeatureContainerColor: Color = Color.Transparent,
  val newFeatureTextColor: Color = Color.Transparent,
  val bgStarColor: Color = Color.Transparent,
  val promoBannerBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
  val promoBannerIconBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors =
  CustomColors(
    // AISU Uganda — light mode teal accents
    appTitleGradientColors = listOf(Color(0xFF4DB8AB), Color(0xFF00796B)),
    tabHeaderBgColor = Color(0xFF00796B),
    taskCardBgColor = surfaceContainerLowestLight,
    taskBgColors =
      listOf(
        // teal
        Color(0xFFEEFAF7),
        // deeper teal
        Color(0xFFE0F5F1),
        // aqua
        Color(0xFFE8FAF5),
        // sage
        Color(0xFFEDF6F4),
      ),
    taskBgGradientColors =
      listOf(
        // bright teal
        listOf(Color(0xFF00BFA5), Color(0xFF00796B)),
        // cyan-teal
        listOf(Color(0xFF26C6DA), Color(0xFF0097A7)),
        // deep teal
        listOf(Color(0xFF4DB8AB), Color(0xFF00695C)),
        // aqua-teal
        listOf(Color(0xFF80CBC4), Color(0xFF00897B)),
      ),
    taskIconColors =
      listOf(
        Color(0xFF00796B),
        Color(0xFF0097A7),
        Color(0xFF00695C),
        Color(0xFF00897B),
      ),
    taskIconShapeBgColor = Color.White,
    homeBottomGradient = listOf(Color(0x00F2FDFB), Color(0xFF9FE8DF)),
    agentBubbleBgColor = Color(0xFFE4F6F3),
    userBubbleBgColor = Color(0xFF00695C),
    linkColor = Color(0xFF00796B),
    successColor = Color(0xFF00796B),
    recordButtonBgColor = Color(0xFF00BFA5),
    waveFormBgColor = Color(0xFFaaaaaa),
    modelInfoIconColor = Color(0xFFCCCCCC),
    warningContainerColor = Color(0xFFFFF8E1),
    warningTextColor = Color(0xFFE65100),
    errorContainerColor = Color(0xFFFCE8E6),
    errorTextColor = Color(0xFFD93025),
    newFeatureContainerColor = Color(0xFFB2DFDB),
    newFeatureTextColor = Color(0xFF004D41),
    bgStarColor = Color(0x3A00C4B0),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.0f to Color(0x4200BFA5),
            0.6154f to Color(0x4200897B),
            1.0f to Color(0x4200695C),
          ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.2442f to Color(0x3B00796B),
            0.4296f to Color(0x3B00BFA5),
            0.6651f to Color(0x3B80CBC4),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
  )

val darkCustomColors =
  CustomColors(
    // AISU Uganda — dark mode, primary accent #00E5C8
    appTitleGradientColors = listOf(Color(0xFF80CBC4), Color(0xFF00E5C8)),
    tabHeaderBgColor = Color(0xFF00E5C8),
    taskCardBgColor = surfaceContainerHighDark,
    taskBgColors =
      listOf(
        // dark teal 1
        Color(0xFF0D1A18),
        // dark teal 2
        Color(0xFF0F1C1A),
        // dark teal 3
        Color(0xFF0C1816),
        // dark teal 4
        Color(0xFF111F1D),
      ),
    taskBgGradientColors =
      listOf(
        // bright teal
        listOf(Color(0xFF00E5C8), Color(0xFF00BFA5)),
        // cyan
        listOf(Color(0xFF26C6DA), Color(0xFF00ACC1)),
        // deeper teal
        listOf(Color(0xFF4DB8AB), Color(0xFF00897B)),
        // aqua
        listOf(Color(0xFF80CBC4), Color(0xFF4DB8AB)),
      ),
    taskIconColors =
      listOf(
        Color(0xFF00E5C8),
        Color(0xFF26C6DA),
        Color(0xFF4DB8AB),
        Color(0xFF80CBC4),
      ),
    taskIconShapeBgColor = Color(0xFF131A17),
    homeBottomGradient = listOf(Color(0x000A0E0B), Color(0x1A00E5C8)),
    agentBubbleBgColor = Color(0xFF0F1511),
    userBubbleBgColor = Color(0xFF00453D),
    linkColor = Color(0xFF60CFBF),
    successColor = Color(0xFF4DB8AB),
    recordButtonBgColor = Color(0xFF00BFA5),
    waveFormBgColor = Color(0xFF5C7E7A),
    modelInfoIconColor = Color(0xFF5C7E7A),
    warningContainerColor = Color(0xFF2A2010),
    warningTextColor = Color(0xFFFCC934),
    errorContainerColor = Color(0xFF3A1818),
    errorTextColor = Color(0xFFF2B8B5),
    newFeatureContainerColor = Color(0xFF00453D),
    newFeatureTextColor = Color(0xFF00E5C8),
    bgStarColor = Color(0x2500E5C8),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops = arrayOf(0.0f to Color(0x6600453D), 0.8077f to Color(0x66002E29)),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.2442f to Color(0x5500796B),
            0.4296f to Color(0x5500BFA5),
            0.6651f to Color(0x5580CBC4),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
  )

val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons based on whether the app is using a dark
 * theme.
 */
@Composable
fun StatusBarColorController(useDarkTheme: Boolean) {
  val view = LocalView.current
  val currentWindow = (view.context as? Activity)?.window

  if (currentWindow != null) {
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
      val controller = WindowCompat.getInsetsController(currentWindow, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme // Set to true for light icons
    }
  }
}

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
  val themeOverride = ThemeSettings.themeOverride
  val darkTheme: Boolean =
    (isSystemInDarkTheme() || themeOverride.value == Theme.THEME_DARK) &&
      themeOverride.value != Theme.THEME_LIGHT
  val view = LocalView.current

  StatusBarColorController(useDarkTheme = darkTheme)

  val colorScheme =
    when {
      darkTheme -> darkScheme
      else -> lightScheme
    }

  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

  CompositionLocalProvider(LocalCustomColors provides customColorsPalette) {
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
  }

  // Make sure the navigation bar stays transparent on manual theme changes.
  LaunchedEffect(darkTheme) {
    val window = (view.context as Activity).window

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
  }
}
