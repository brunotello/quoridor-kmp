package com.btello.quoridor

import com.btello.quoridor.presentation.settings.LANGUAGE_ENGLISH
import com.btello.quoridor.presentation.settings.LANGUAGE_SPANISH
import com.btello.quoridor.presentation.settings.defaultLanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultLanguageCodeTest {

    @Test
    fun usesSpanishWhenDeviceIsSpanish() {
        assertEquals(LANGUAGE_SPANISH, defaultLanguageCode("es"))
    }

    @Test
    fun usesSpanishForSpanishRegionalVariants() {
        assertEquals(LANGUAGE_SPANISH, defaultLanguageCode("es-AR"))
        assertEquals(LANGUAGE_SPANISH, defaultLanguageCode("ES"))
    }

    @Test
    fun usesEnglishWhenDeviceIsEnglish() {
        assertEquals(LANGUAGE_ENGLISH, defaultLanguageCode("en"))
        assertEquals(LANGUAGE_ENGLISH, defaultLanguageCode("en-US"))
    }

    @Test
    fun fallsBackToEnglishForUnsupportedLanguages() {
        assertEquals(LANGUAGE_ENGLISH, defaultLanguageCode("fr"))
        assertEquals(LANGUAGE_ENGLISH, defaultLanguageCode("pt"))
        assertEquals(LANGUAGE_ENGLISH, defaultLanguageCode(""))
    }
}
