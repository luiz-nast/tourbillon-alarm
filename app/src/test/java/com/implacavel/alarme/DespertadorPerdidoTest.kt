package com.implacavel.alarme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A janela do despertador perdido (celular desligado na hora): toca quando ele volta, até 12 h depois (Agendador.perdido). */
class DespertadorPerdidoTest {
    private val agora = 1_800_000_000_000L
    private val dozeHoras = 12 * 60 * 60_000L

    @Test
    fun aJanelaE12Horas() = assertEquals(dozeHoras, Agendador.JANELA_PERDIDO_MS)

    @Test
    fun acabouDePassarToca() = assertTrue(Agendador.perdido(agora - 1_000, agora))

    @Test
    fun naHoraExataToca() = assertTrue(Agendador.perdido(agora, agora))

    @Test
    fun exatamenteDozeHorasAindaToca() = assertTrue(Agendador.perdido(agora - dozeHoras, agora))

    @Test
    fun dozeHorasE1MsJaNaoToca() = assertFalse(Agendador.perdido(agora - dozeHoras - 1, agora))

    @Test
    fun horaNoFuturoNaoToca() = assertFalse(Agendador.perdido(agora + 1, agora))

    @Test
    fun semHoraGuardadaNaoToca() = assertFalse(Agendador.perdido(0, agora))
}
