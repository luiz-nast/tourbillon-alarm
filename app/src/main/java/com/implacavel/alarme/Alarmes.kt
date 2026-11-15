package com.implacavel.alarme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Lista de alarmes salvos, fonte única da verdade: a tela observa [lista]; receivers e serviço
 * leem e gravam por aqui. Tudo roda na thread principal, então não há concorrência.
 * Grava JSON em [prefsProtegidas], que pode ser lido antes do primeiro desbloqueio.
 */
object Alarmes {
    private lateinit var prefs: SharedPreferences
    private val _lista = MutableStateFlow<List<Alarme>>(emptyList())
    val lista: StateFlow<List<Alarme>> = _lista.asStateFlow()

    /** Chamado uma vez em [App.onCreate], antes de qualquer outro uso. */
    fun carregar(ctx: Context) {
        prefs = ctx.prefsProtegidas("alarmes")
        _lista.value = lerAlarmes(prefs.getString("lista", null)) { Log.e(TAG, "Alarmes: entrada ilegível, ignorada", it) }
    }

    fun buscar(id: Int): Alarme? = _lista.value.firstOrNull { it.id == id }

    fun novoId(): Int = (_lista.value.maxOfOrNull { it.id } ?: 0) + 1

    /** Insere ou substitui (pelo id), mantendo a lista em ordem de horário. */
    fun salvar(alarme: Alarme) =
        gravar((_lista.value.filter { it.id != alarme.id } + alarme).sortedWith(compareBy({ it.hora }, { it.minuto })))

    fun remover(id: Int) = gravar(_lista.value.filter { it.id != id })

    private fun gravar(lista: List<Alarme>) {
        // commit e não apply: receivers podem ser encerrados pelo sistema logo em seguida
        prefs.edit(commit = true) { putString("lista", JSONArray(lista.map { it.paraJson() }).toString()) }
        _lista.value = lista
    }
}

/**
 * Os alarmes do JSON salvo em [texto]. Entrada ilegível (ou o JSON todo) fica de fora, avisando [falhou]: uma só não
 * pode derrubar o [App] a cada partida, porque aí nenhum alarme tocaria. Quem lê não regrava, então nada se perde até o próximo salvar.
 */
internal fun lerAlarmes(texto: String?, falhou: (Throwable) -> Unit = {}): List<Alarme> {
    val json = runCatching { JSONArray(texto ?: "[]") }.getOrElse { falhou(it); return emptyList() }
    // Um tipo só por alarme (versões antigas deixavam missão e remédio juntos, ou nenhum); a caminhada só vale com missão
    return (0 until json.length()).mapNotNull { i ->
        runCatching { Alarme.deJson(json.getJSONObject(i)).normalizado() }.onFailure(falhou).getOrNull()
    }
}
