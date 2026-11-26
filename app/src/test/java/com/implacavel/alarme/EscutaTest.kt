package com.implacavel.alarme

import org.junit.Assert.assertEquals
import org.junit.Test

/** O que a Escuta entrega à IA (RegrasConversa.kt). */
class EscutaTest {
    @Test
    fun aFalaVaiComAsOutrasHipotesesDoReconhecedor() {
        assertEquals("Nice, I slept well", falaComAlternativas("Nice, I slept well", emptyList()))
        // Iguais à fala (mesmo de outro jeito) e repetidas ficam de fora; no máximo duas
        assertEquals("Nice, I slept well", falaComAlternativas("Nice, I slept well", listOf("nice, i slept well", " Nice, I slept well ")))
        assertEquals(
            "Nice, I slept well\n(The speech recognizer also heard: \"Knife, I slept well\" / \"Nice, I slept while\".)",
            falaComAlternativas("Nice, I slept well", listOf("Knife, I slept well", "Nice, I slept while", "Knife, I slept well", "Mice, I slept well")),
        )
    }
}
