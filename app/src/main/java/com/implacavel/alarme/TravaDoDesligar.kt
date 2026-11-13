// A trava do desligar pelo Knox (RestrictionPolicy.allowPowerOff), só no Samsung, sem o knoxsdk.jar: as classes
// com.samsung.android.knox.* já vêm no sistema e são chamadas por reflexão. Exige o app administrador (AdminDoAlarme.kt)
// e uma licença KPE. Tudo só pelo cabo (permissão DUMP, como o PararPeloCabo); a chave nunca vai pro repo nem pro log:
//   adb shell am broadcast -n com.implacavel.alarme/.TravaPeloCabo --es chave <chave KPE>
//   adb shell am broadcast -n com.implacavel.alarme/.TravaPeloCabo --es comando liberar     (ou travar)
// Não há botão de liberar na tela: o app é contra a autossabotagem.
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

    /** O que o cabo pediu: travar (o Knox recebe false, não deixa desligar), liberar (true) ou nada. */
    fun permitir(comando: String?): Boolean? = when (comando) {
        "travar" -> false
        "liberar" -> true
        else -> null
    }

    private val restricoes get() = Class.forName("com.samsung.android.knox.restriction.RestrictionPolicy")

    private fun politica(ctx: Context): Any {
        val gerente = Class.forName("com.samsung.android.knox.EnterpriseDeviceManager")
        val edm = gerente.getMethod("getInstance", Context::class.java).invoke(null, ctx)
        return gerente.getMethod("getRestrictionPolicy").invoke(edm)!!
    }

    /** Trava (false) ou libera (true) o desligar: a tecla lateral, o painel rápido e o reset de fábrica. */
    fun permitirDesligar(ctx: Context, permitir: Boolean): Boolean =
        tentar { restricoes.getMethod("allowPowerOff", Boolean::class.javaPrimitiveType).invoke(politica(ctx), permitir) as Boolean }
            .onFailure { Log.w(TAG, "Knox: allowPowerOff($permitir) falhou (${it.javaClass.simpleName}: ${it.message})") }
            .getOrDefault(false)

    /** O que o Knox diz agora; null se não deu pra perguntar. */
    fun desligarPermitido(ctx: Context): Boolean? =
        tentar { restricoes.getMethod("isPowerOffAllowed").invoke(politica(ctx)) as Boolean }.getOrNull()

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
        TravaDoDesligar.permitir(intent.getStringExtra("comando"))?.let {
            Log.w(TAG, "Knox: ${if (it) "liberar" else "travar"} o desligar -> ${TravaDoDesligar.permitirDesligar(ctx, it)}")
        }
        Log.w(TAG, "Knox: desligar permitido = ${TravaDoDesligar.desligarPermitido(ctx) ?: "sem resposta"}")
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
