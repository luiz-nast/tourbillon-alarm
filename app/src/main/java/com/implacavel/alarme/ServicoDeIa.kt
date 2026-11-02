// O Gemma do Tourbillon aberto pro Chiron Routine, o outro app do dono (11-02: "o mesmo modelo compartilhado entre o
// Tourbillon e o Chiron"). O modelo (2,6 GB) fica só aqui; o Chiron manda a instrução dele e a frase da pessoa, e
// recebe a resposta (uma linha JSON com a habilidade escolhida). Uma só cópia do modelo na memória.
//
// Só entra quem tem a permissão com.implacavel.alarme.permission.IA, de assinatura (só apps com a mesma chave deste).
// O alarme sempre passa na frente: com um alarme tocando (ou em andamento, ou o teste da IA pelo cabo), a resposta
// volta vazia na hora e o Chiron se vira com as regras dele. Sem uso por PARADO_MS, o modelo sai da memória.
// O protocolo está no README do Chiron (docs/IA-compartilhada.md).
package com.implacavel.alarme

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log

class ServicoDeIa : Service() {
    companion object {
        const val PERGUNTAR = 1 // Chiron → aqui: Bundle("instrucao", "prompt"), replyTo
        const val RESPOSTA = 2 // aqui → Chiron: Bundle("texto"); vazio = não deu
        const val AQUECER = 3 // Chiron → aqui: Bundle("instrucao"); carrega o modelo e lê a instrução, sem resposta
        private const val INSTRUCAO_MAX = 8_000 // letras: uma instrução maior que isso não é do Chiron
        private const val PERGUNTA_MAX = 1_000

        /** Sem pergunta nenhuma por este tempo (e sem alarme), o modelo sai da memória. */
        private const val PARADO_MS = 3 * 60_000L
    }

    private val principal = Handler(Looper.getMainLooper())
    private var pedidos = 0 // a cada pergunta: só a última agenda a saída do modelo

    private val soltar = Runnable {
        // Só sem alarme: a conversa da manhã usa o mesmo modelo, e quem solta no fim dela é o AlarmeService
        if (!alarmeNaFrente()) {
            Log.i(TAG, "IA do celular: o Chiron parou de perguntar, modelo sai da memória")
            ModeloLocal.liberar()
        }
    }

    private val mensageiro = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            PERGUNTAR -> atender(msg)
            AQUECER -> aquecer(msg)
        }
        true
    })

    override fun onBind(intent: Intent?): IBinder = mensageiro.binder

    /** Um alarme está tocando, em andamento ou o teste da IA está com o modelo: o alarme passa na frente. */
    private fun alarmeNaFrente(): Boolean =
        AlarmeService.tocando.value != null || Ajustes.emAndamento != null || ModeloLocal.emTeste

    /** O Chiron abriu: o modelo carrega e lê a instrução dele já, pra a primeira pergunta não levar 20 s. */
    private fun aquecer(msg: Message) {
        val instrucao = msg.data?.getString("instrucao").orEmpty().take(INSTRUCAO_MAX)
        if (instrucao.isBlank() || alarmeNaFrente() || !ModeloLocal.disponivel(this)) return
        val numero = ++pedidos
        principal.removeCallbacks(soltar)
        ModeloLocal.perguntaAvulsa(this, instrucao, null) { if (numero == pedidos) principal.postDelayed(soltar, PARADO_MS) }
    }

    private fun atender(msg: Message) {
        val quem = msg.replyTo ?: return
        val instrucao = msg.data?.getString("instrucao").orEmpty().take(INSTRUCAO_MAX)
        val pergunta = msg.data?.getString("prompt").orEmpty().take(PERGUNTA_MAX)
        fun devolver(texto: String?) {
            val resposta = Message.obtain(null, RESPOSTA).apply { data = Bundle().apply { putString("texto", texto.orEmpty()) } }
            runCatching { quem.send(resposta) }
        }
        if (pergunta.isBlank()) return devolver(null)
        if (alarmeNaFrente()) {
            Log.i(TAG, "IA do celular: o Chiron perguntou com alarme em andamento, fica sem resposta")
            return devolver(null)
        }
        if (!ModeloLocal.disponivel(this)) {
            Log.i(TAG, "IA do celular: o Chiron perguntou, mas o modelo não está disponível (arquivo ou memória)")
            return devolver(null)
        }
        val numero = ++pedidos
        principal.removeCallbacks(soltar)
        ModeloLocal.perguntaAvulsa(this, instrucao, pergunta) { texto ->
            devolver(texto)
            if (numero == pedidos) principal.postDelayed(soltar, PARADO_MS)
        }
    }

    override fun onDestroy() {
        // O Chiron desligou: o modelo sai daqui a pouco, se ninguém mais o usar (o soltar confere o alarme)
        principal.removeCallbacks(soltar)
        if (!alarmeNaFrente()) principal.postDelayed({ if (!alarmeNaFrente()) ModeloLocal.liberar() }, PARADO_MS)
        super.onDestroy()
    }
}
