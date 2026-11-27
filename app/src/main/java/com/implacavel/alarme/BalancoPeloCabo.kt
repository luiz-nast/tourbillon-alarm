// Um balanço de mentira, só pelo cabo (permissão DUMP, como o PararPeloCabo): pra ver a tela do balanço e o bipe calando
// sem sacudir o celular. Soma ao acelerômetro de verdade uma onda de 3 vezes por segundo desta amplitude (m/s²); some
// sozinho depois dos segundos pedidos (no máximo 60):
//   adb shell am broadcast -n com.implacavel.alarme/.BalancoPeloCabo --ef amplitude 25 --ei segundos 10
// Com o celular travado, sem os botões de teste à mão, --ez tocar true toca o alarme de teste da câmera daqui a 5 s; com
// --ez mudo true, sem som e sem vibrar (o mudo some no fim do alarme ou em 10 min, o que vier antes). --ez stop true
// é o STOP dito, pra quem não pode falar perto do celular.
package com.implacavel.alarme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class BalancoPeloCabo : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.getBooleanExtra("stop", false)) return AlarmeService.silenciar(ctx)
        if (intent.getBooleanExtra("tocar", false)) {
            mudoPeloCabo = intent.getBooleanExtra("mudo", false)
            principal.reagendar(fimDoMudo, MUDO_MAX_MS)
            return Agendador.agendarTeste(ctx, 5)
        }
        balancoDeTeste = intent.getFloatExtra("amplitude", 0f)
        val segundos = intent.getIntExtra("segundos", 10).coerceIn(1, 60)
        Log.w(TAG, "Pelo cabo: balanço de mentira de $balancoDeTeste m/s² por $segundos s")
        principal.reagendar(fimDoBalanco, segundos * 1000L) // um balanço novo troca o anterior, e o prazo é o dele
    }

    private companion object {
        val principal = Handler(Looper.getMainLooper())
        val fimDoBalanco = Runnable { balancoDeTeste = 0f }
        val fimDoMudo = Runnable { mudoPeloCabo = false }
        const val MUDO_MAX_MS = 10 * 60_000L
    }
}
