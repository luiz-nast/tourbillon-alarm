// O modo da manhã aberto pro Mistral, o outro app do dono. (o dono, 11-08): "durante a atividade da manhã do
// Tourbillon, ele controla o Mistral". Só leitura, com a mesma permissão de assinatura da [FonteDeSons]:
//   content://com.implacavel.alarme.modo   uma linha: etapa (CONVERSA, CAMINHADA, CAMERA ou nenhuma) e valeAte
// (epoch, ms: agora + 90 s na atividade, 0 fora dela; [modoDaManha], [modoValeAte]). Quem decide é o Mistral, e ele
// falha fechado: sem resposta, etapa desconhecida ou valeAte vencido, valem as regras normais dele.
package com.implacavel.alarme

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class FonteDoModo : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, ordem: String?): Cursor {
        // a etapa fingida pelo cabo só vale sem alarme de verdade: a manhã real sempre manda
        val modo = fingido?.takeIf { AlarmeService.tocando.value == null && System.currentTimeMillis() < it.second }?.first
            ?: modoDaManha(emAtividade = AlarmeService.tocando.value?.missao == true, fase = faseAgora())
        return MatrixCursor(arrayOf("etapa", "valeAte")).apply { addRow(arrayOf<Any>(modo, modoValeAte(modo, System.currentTimeMillis()))) }
    }

    /** A fase que a tela do alarme mostra (a mesma conta da AlarmeActivity), pelo que o serviço publica; null na música, no bom dia e na comemoração. */
    private fun faseAgora(): FaseDoTeste? = with(AlarmeService) {
        if (!silenciado.value || saudando.value || comemorando.value) null
        else faseDoTeste(
            conversa = conversa.value != null, caminhada = caminhada.value != null, noPrazo = false,
            verificacao = verificacao.value != null, metadeVerificada = false, naCamera = !comCaminhada.value,
        )
    }

    companion object {
        /** (o dono, 11-08): o teste pelo cabo finge uma etapa até a hora dada, sem alarme nem som (TesteDaIa --es modo). */
        @Volatile var fingido: Pair<String, Long>? = null
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int = 0
}
