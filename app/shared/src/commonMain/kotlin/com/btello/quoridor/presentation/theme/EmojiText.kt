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
import com.btello.quoridor.getPlatform

/**
 * Obtiene la familia de fuentes para renderizar emojis de forma consistente.
 *
 * - Android: Usa la fuente personalizada Noto Color Emoji que está empaquetada
 * - iOS: Usa el fallback del sistema (FontFamily.Default) que renderiza emojis nativamente
 */
@Composable
fun rememberEmojiFontFamily(): FontFamily {
    val platform = getPlatform()
    // En iOS, no forzamos una fuente personalizada para permitir el fallback del sistema
    return if (platform.name.startsWith("iOS")) {
        FontFamily.Default
    } else {
        // Android y otras plataformas usan la fuente personalizada
        FontFamily(Font(Res.font.noto_color_emoji))
    }
}

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
