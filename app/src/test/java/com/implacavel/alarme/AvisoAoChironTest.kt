package com.implacavel.alarme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** O limite de 1 aviso por minuto da missão viva (AvisoAoChiron.kt). */
class AvisoAoChironTest {
    private val t0 = 1_000_000L

    @Test
    fun oPrimeiroAvisoSai() = assertTrue(AvisoAoChiron.deveAvisar(t0, 0L))

    @Test
    fun cinquentaENoveSegundosDepoisNaoSai() = assertFalse(AvisoAoChiron.deveAvisar(t0 + 59_000, t0))

    @Test
    fun sessentaSegundosDepoisSai() = assertTrue(AvisoAoChiron.deveAvisar(t0 + 60_000, t0))
}
