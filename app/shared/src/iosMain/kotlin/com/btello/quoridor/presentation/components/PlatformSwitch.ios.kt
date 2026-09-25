package com.btello.quoridor.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch
import platform.darwin.NSObject

/**
 * Recibe el evento `valueChanged` del [UISwitch] nativo y lo reenvia a Compose.
 * Se guarda un target por composicion para que la accion ObjC no se libere.
 */
@OptIn(BetaInteropApi::class)
private class SwitchTarget(var onChange: (Boolean) -> Unit) : NSObject() {
    @ObjCAction
    fun valueChanged(sender: UISwitch) {
        onChange(sender.on)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    containerColor: Color,
) {
    val target = remember { SwitchTarget(onCheckedChange) }
    target.onChange = onCheckedChange

    val uiSwitch = remember {
        UISwitch().apply {
            addTarget(
                target = target,
                action = NSSelectorFromString("valueChanged:"),
                forControlEvents = UIControlEventValueChanged,
            )
        }
    }

    // Las esquinas transparentes del UISwitch dejarian ver el fondo del interop
    // (blanco); lo pintamos con el color del contenedor Compose para que se funda.
    uiSwitch.backgroundColor = containerColor.toUIColor()

    // El tamano del UISwitch cambia entre versiones de iOS (en iOS 26 es mayor);
    // usamos su tamano intrinseco real para no recortarlo.
    val size = remember {
        uiSwitch.intrinsicContentSize.useContents { width to height }
    }

    UIKitView(
        factory = { uiSwitch },
        modifier = modifier.size(width = size.first.dp, height = size.second.dp),
        update = { switch -> switch.setOn(checked, animated = true) },
    )
}

private fun Color.toUIColor(): UIColor =
    if (this == Color.Unspecified) {
        UIColor.clearColor
    } else {
        UIColor(
            red = red.toDouble(),
            green = green.toDouble(),
            blue = blue.toDouble(),
            alpha = alpha.toDouble(),
        )
    }
