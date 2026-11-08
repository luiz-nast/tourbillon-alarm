package com.implacavel.alarme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O contrato do motor compartilhado (ContratoDoMotor.kt). */
class ContratoDoMotorTest {

    /** Tokens de uma saída, estimados como no Gemma em português e JSON: ~3,5 letras por token. */
    private fun tokens(saida: String) = Math.ceil(saida.length / 3.5).toInt()

    @Test
    fun tetoDaPerguntaCabeAMaiorRespostaComFolgaENaoPassaDe64() {
        // A maior saída do modelo ao Chiron na bateria 1b (339 respostas, 11-08): 111 letras, ~32 tokens
        val maiorDaBateria = "```json { \"acao\": \"definir\", \"texto\": \"tomar vitamina\", \"repete\": \"diaria\", \"som\": false, \"hora\": \"08:00\" } ```"
        // Desde o 88df146 do Chiron, o "conversar" traz a fala de até 20 palavras: 20 de 6 letras (acima da média do português)
        val fala = List(20) { "abcdef" }.joinToString(" ")
        val conversar = "```json { \"acao\": \"conversar\", \"fala\": \"$fala\" } ```"
        for (saida in listOf(maiorDaBateria, conversar)) {
            assertTrue("${tokens(saida)} tokens: $saida", tokens(saida) <= AVULSA_MAX_TOKENS * 9 / 10)
        }
        // (o dono, 11-08): 120 -> ~60, o pior caso cai de ~8 s pra ~4 s (17 a 19 tokens por segundo na GPU)
        assertTrue(AVULSA_MAX_TOKENS <= 64)
    }

    @Test
    fun prazoVencidoNaoGeraESemPrazoFicaComoAntes() {
        val agora = 1_000_000L
        assertEquals("prazo vencido", semVez(alarme = false, ate = agora - 1, agora = agora))
        assertNull(semVez(alarme = false, ate = agora + 10_000, agora = agora))
        assertNull(semVez(alarme = false, ate = 0L, agora = agora)) // sem a chave "ate": como antes
    }

    @Test
    fun alarmeNaFrenteNaoGeraMesmoNoPrazo() {
        val agora = 1_000_000L
        assertEquals("alarme na frente", semVez(alarme = true, ate = agora + 10_000, agora = agora))
        assertEquals("alarme na frente", semVez(alarme = true, ate = 0L, agora = agora))
    }
}
