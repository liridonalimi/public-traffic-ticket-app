package com.buspay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun systemSelectionUsesAlbanianWhenDeviceUsesAlbanian() {
        assertEquals("sq", resolvedLanguageTag(AppLanguage.SYSTEM, "sq-XK"))
    }

    @Test
    fun systemSelectionFallsBackToEnglishForUnsupportedDeviceLanguage() {
        assertEquals("en", resolvedLanguageTag(AppLanguage.SYSTEM, "de-DE"))
    }

    @Test
    fun explicitSelectionOverridesDeviceLanguage() {
        assertEquals("sq", resolvedLanguageTag(AppLanguage.ALBANIAN, "en-US"))
        assertEquals("en", resolvedLanguageTag(AppLanguage.ENGLISH, "sq-XK"))
    }
}
