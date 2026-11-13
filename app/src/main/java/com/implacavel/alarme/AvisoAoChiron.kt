package com.implacavel.alarme

import android.content.Context
import android.content.Intent

/**
 * Conta ao Chiron o próximo despertador e a missão viva, pra ele pôr tudo na agenda do relógio (plano B
 * se o celular desligar). Sem o Chiron instalado, não acontece nada.
 */
object AvisoAoChiron {
    const val PACOTE = "io.github.luiz_nast.chiron.routine"
    const val ACAO = "$PACOTE.DESPERTADOR"
    const val MISSAO = "tourbillon-missao"

    /** A missão conta como viva por este tempo depois de cada aviso: se o celular sumir, o relógio toca. */
    const val VIVA_MS = 5 * 60_000L
    const val INTERVALO_MS = 60_000L

    @Volatile var ultimo = 0L

    fun deveAvisar(agora: Long, ultimo: Long) = agora - ultimo >= INTERVALO_MS

    fun despertador(ctx: Context, id: Int, hora: Long, nome: String) = enviar(ctx, "tourbillon-$id", hora, nome)

    fun missaoViva(ctx: Context, agora: Long) {
        if (!deveAvisar(agora, ultimo)) return
        ultimo = agora
        enviar(ctx, MISSAO, agora + VIVA_MS, "")
    }

    fun missaoAcabou(ctx: Context) {
        ultimo = 0
        enviar(ctx, MISSAO, 0, "")
    }

    private fun enviar(ctx: Context, id: String, hora: Long, nome: String) {
        runCatching { ctx.sendBroadcast(Intent(ACAO).setPackage(PACOTE).putExtra("id", id).putExtra("hora", hora).putExtra("nome", nome)) }
    }
}
