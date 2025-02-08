package launcher.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.res.fontResource
import launcher.launcher.R

// JetBrains Mono Font Family
val JetBrainsMonoFont = FontFamily(
    Font(R.font.jetbrains_regular, FontWeight.Normal)
)

val customTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMonoFont,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = JetBrainsMonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )

)