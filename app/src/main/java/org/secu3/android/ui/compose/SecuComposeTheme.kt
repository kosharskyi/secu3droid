package org.secu3.android.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Semantic colors for ECU and system feedback. Pair each foreground with its matching container. */
data class SecuPalette(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

/** Read the shared success, warning, and information colors from any themed screen. */
val LocalSecuPalette = staticCompositionLocalOf {
    SecuPalette(
        success = Color(0xFF247447), onSuccess = Color.White,
        successContainer = Color(0xFFD5F2DF), onSuccessContainer = Color(0xFF123D26),
        warning = Color(0xFF875500), onWarning = Color.White,
        warningContainer = Color(0xFFFFE7B2), onWarningContainer = Color(0xFF503100),
        info = Color(0xFF1769AA), onInfo = Color.White,
        infoContainer = Color(0xFFD7EAFE), onInfoContainer = Color(0xFF102F4A),
    )
}

private val LocalResolvedDarkTheme = staticCompositionLocalOf<Boolean?> { null }

private val LightColors = lightColorScheme(
    primary = Color(0xFF1769AA), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EAFE), onPrimaryContainer = Color(0xFF102F4A),
    inversePrimary = Color(0xFF9BCDF5),
    secondary = Color(0xFF526575), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E8EF), onSecondaryContainer = Color(0xFF263846),
    tertiary = Color(0xFF2D687E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDEAF5), onTertiaryContainer = Color(0xFF123746),
    background = Color(0xFFF3F6FA), onBackground = Color(0xFF152331),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF152331),
    surfaceDim = Color(0xFFD7DFE7), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF0F4F8), surfaceContainerHigh = Color(0xFFE9EFF4),
    surfaceContainerHighest = Color(0xFFE2EAF0), surfaceTint = Color(0xFF1769AA),
    surfaceVariant = Color(0xFFE8EEF3), onSurfaceVariant = Color(0xFF526575),
    outline = Color(0xFF7B8D9B), outlineVariant = Color(0xFFC7D2DB),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2B3A47), inverseOnSurface = Color(0xFFF2F5F8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF79BBF1), onPrimary = Color(0xFF082B45),
    primaryContainer = Color(0xFF184C74), onPrimaryContainer = Color(0xFFD7EAFE),
    inversePrimary = Color(0xFF1769AA),
    secondary = Color(0xFFBDCBD6), onSecondary = Color(0xFF263846),
    secondaryContainer = Color(0xFF354755), onSecondaryContainer = Color(0xFFE0E8EF),
    tertiary = Color(0xFF9CCFE0), onTertiary = Color(0xFF123746),
    tertiaryContainer = Color(0xFF214F60), onTertiaryContainer = Color(0xFFCDEAF5),
    background = Color(0xFF111820), onBackground = Color(0xFFEEF4F8),
    surface = Color(0xFF1A2530), onSurface = Color(0xFFEEF4F8),
    surfaceDim = Color(0xFF111820), surfaceBright = Color(0xFF303C47),
    surfaceContainerLowest = Color(0xFF0C1218), surfaceContainerLow = Color(0xFF17212B),
    surfaceContainer = Color(0xFF1A2530), surfaceContainerHigh = Color(0xFF222E39),
    surfaceContainerHighest = Color(0xFF2A3742), surfaceTint = Color(0xFF79BBF1),
    surfaceVariant = Color(0xFF293743), onSurfaceVariant = Color(0xFFBDCBD6),
    outline = Color(0xFF8798A5), outlineVariant = Color(0xFF455662),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFEEF4F8), inverseOnSurface = Color(0xFF263846),
)

private val LightPalette = SecuPalette(
    success = Color(0xFF247447), onSuccess = Color.White,
    successContainer = Color(0xFFD5F2DF), onSuccessContainer = Color(0xFF123D26),
    warning = Color(0xFF875500), onWarning = Color.White,
    warningContainer = Color(0xFFFFE7B2), onWarningContainer = Color(0xFF503100),
    info = Color(0xFF1769AA), onInfo = Color.White,
    infoContainer = Color(0xFFD7EAFE), onInfoContainer = Color(0xFF102F4A),
)
private val DarkPalette = SecuPalette(
    success = Color(0xFF8BD5A5), onSuccess = Color(0xFF103A21),
    successContainer = Color(0xFF1D5633), onSuccessContainer = Color(0xFFD5F2DF),
    warning = Color(0xFFFFC66D), onWarning = Color(0xFF482B00),
    warningContainer = Color(0xFF654000), onWarningContainer = Color(0xFFFFE7B2),
    info = Color(0xFF79BBF1), onInfo = Color(0xFF082B45),
    infoContainer = Color(0xFF184C74), onInfoContainer = Color(0xFFD7EAFE),
)

private val SecuShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Shared Material 3 theme. Nested default calls inherit the root's resolved appearance. */
@Composable
fun SecuComposeTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val inherited = LocalResolvedDarkTheme.current
    val resolvedDark = darkTheme ?: inherited ?: isSystemInDarkTheme()
    val palette = if (resolvedDark) DarkPalette else LightPalette
    val colorScheme = if (resolvedDark) DarkColors else LightColors
    CompositionLocalProvider(LocalResolvedDarkTheme provides resolvedDark, LocalSecuPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography().let { defaults ->
                defaults.copy(labelSmall = defaults.labelSmall.copy(fontSize = 12.sp))
            },
            shapes = SecuShapes,
        ) {
            CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
        }
    }
}
