package com.btello.quoridor.presentation.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.Font
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.noto_color_emoji

/**
 * Fuente de emojis a color empaquetada con la app.
 *
 * Los targets basados en Skiko (iOS, escritorio y web) no tienen un fallback de
 * emojis del sistema, por lo que sin esta fuente los emojis se renderizan como
 * "?". Empaquetamos un subconjunto de Noto Color Emoji para que se vean igual en
 * todas las plataformas.
 */
@Composable
fun rememberEmojiFontFamily(): FontFamily = FontFamily(Font(Res.font.noto_color_emoji))

/**
 * [Text] que renderiza emojis usando [rememberEmojiFontFamily]. Mantiene el resto
 * del estilo (tamaño, etc.) del tema, sólo sustituye la familia tipográfica por la
 * de emojis a color.
 */
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFamily = rememberEmojiFontFamily()),
    )
}

@Preview
@Composable
private fun EmojiTextPreview() {
    QuoridorTheme {
        EmojiText(text = "🤖 👥 ⭐")
    }
}
