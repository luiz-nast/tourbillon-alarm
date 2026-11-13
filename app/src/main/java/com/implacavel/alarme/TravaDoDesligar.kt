// A trava do desligar pelo Knox (RestrictionPolicy.allowPowerOff), só no Samsung, sem o knoxsdk.jar: as classes
// com.samsung.android.knox.* já vêm no sistema e são chamadas por reflexão. Exige o app administrador (AdminDoAlarme.kt)
// e uma licença KPE. Tudo só pelo cabo (permissão DUMP, como o PararPeloCabo); a chave nunca vai pro repo nem pro log:
//   adb shell am broadcast -n com.implacavel.alarme/.TravaPeloCabo --es chave <chave KPE>
//   adb logcat -s Implacavel
// Fora da Samsung, sem licença ou sem administrador, a chamada falha, o log conta e o app segue.
// Veja docs/analise-desligamento.md.
package com.implacavel.alarme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.lang.reflect.InvocationTargetException

object TravaDoDesligar {
    /** Uma chamada ao Knox por reflexão: nunca lança, e o erro de dentro (ex.: SecurityException) vem desembrulhado. */
    fun <T> tentar(bloco: () -> T): Result<T> = try {
        Result.success(bloco())
    } catch (e: Throwable) {
        Result.failure((e as? InvocationTargetException)?.targetException ?: e)
    }

    /** O Knox responde no [LicencaDoKnox]. A chave não fica guardada: pra ativar de novo, ela vem de novo pelo cabo. */
    fun ativarLicenca(ctx: Context, chave: String) {
        tentar {
            val licencas = Class.forName("com.samsung.android.knox.license.KnoxEnterpriseLicenseManager")
            val gerente = licencas.getMethod("getInstance", Context::class.java).invoke(null, ctx)
            licencas.getMethod("activateLicense", String::class.java).invoke(gerente, chave)
        }
            .onSuccess { Log.w(TAG, "Knox: licença pedida, o resultado vem a seguir") }
            // Só o tipo do erro: a mensagem poderia trazer a chave
            .onFailure { Log.w(TAG, "Knox: a licença não foi pedida (${it.javaClass.simpleName})") }
    }
}

/** Os pedidos do cabo. Só o adb tem a permissão DUMP. */
class TravaPeloCabo : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        intent.getStringExtra("chave")?.let { TravaDoDesligar.ativarLicenca(ctx, it) }
    }
}

/** O resultado da licença, que o Knox manda só a este app. Loga o código, nunca a chave. */
class LicencaDoKnox : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val erro = intent.getIntExtra("com.samsung.android.knox.intent.extra.KNOX_LICENSE_ERROR_CODE", -1)
        // 800 ativação, 801 validação, 802 desativação
        val tipo = intent.getIntExtra("com.samsung.android.knox.intent.extra.KNOX_LICENSE_RESULT_TYPE", -1)
        Log.w(TAG, if (erro == 0) "Knox: licença ok (tipo $tipo)" else "Knox: licença com erro $erro (tipo $tipo)")
    }
}
