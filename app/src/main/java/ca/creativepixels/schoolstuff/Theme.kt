package ca.creativepixels.schoolstuff

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF153E6B)
val SchoolBlue = Color(0xFF3E82D7)
val SchoolYellow = Color(0xFFFFD56B)
val SchoolGreen = Color(0xFF70C78A)
val SchoolPink = Color(0xFFF2A9BE)
val SchoolRed = Color(0xFFE64B5C)
val Paper = Color(0xFFFFFCF8)
val SoftBlue = Color(0xFFEAF3FF)
val SoftGreen = Color(0xFFEDF9F0)
val SoftPink = Color(0xFFFFEEF3)
val SoftYellow = Color(0xFFFFF7D9)
val SoftPurple = Color(0xFFF5EFFD)

private val SchoolColorScheme = lightColorScheme(
    primary = SchoolBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = Ink,
    secondary = SchoolGreen,
    secondaryContainer = SoftGreen,
    tertiary = SchoolPink,
    tertiaryContainer = SoftPink,
    background = Paper,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    error = SchoolRed
)

@Composable
fun SchoolStuffTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SchoolColorScheme, content = content)
}
