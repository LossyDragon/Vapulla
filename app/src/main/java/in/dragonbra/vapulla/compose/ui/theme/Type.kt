package `in`.dragonbra.vapulla.compose.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import `in`.dragonbra.vapulla.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val ropaSans = GoogleFont("Ropa Sans")

val fontFamily = FontFamily(Font(ropaSans, provider))
