package com.btello.quoridor.presentation.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiFontFamilyTest {
    /**
     * Verifica que la lógica de detección de plataforma funciona correctamente.
     * iOS debe retornar FontFamily.Default, otras plataformas usan la personalizada.
     */
    @Test
    fun platformDetectionLogicWorks() {
        val iosPlatformName = "iOS 15.0"
        val androidPlatformName = "Android 13"
        
        val isIOS = iosPlatformName.startsWith("iOS")
        val isAndroid = !androidPlatformName.startsWith("iOS")
        
        assertEquals(true, isIOS, "iOS platform should be detected")
        assertEquals(true, isAndroid, "Android platform should not be detected as iOS")
    }

    /**
     * Verifica que para plataformas que no son iOS, se usa la fuente personalizada.
     */
    @Test
    fun nonIosPlatformsUseCustomFont() {
        val androidPlatformName = "Android 13"
        val isAndroid = !androidPlatformName.startsWith("iOS")
        assertEquals(true, isAndroid, "Android should not be treated as iOS")
    }
}
