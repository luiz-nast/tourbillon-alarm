// A biblioteca de sons ([Sons]) aberta pro Maverick Truck, o outro app do dono (10-31): a mesma base de músicas nos dois.
// Só entra quem tem a permissão com.implacavel.alarme.permission.SONS, que é de assinatura: só apps assinados com a mesma
// chave deste. O Maverick lista, toca, adiciona e apaga:
//   content://com.implacavel.alarme.sons            lista (colunas nome, tamanho)
//   content://com.implacavel.alarme.sons/som/NOME   abrir pra ler ("r") e apagar
//   content://com.implacavel.alarme.sons/novo/NOME  abrir pra gravar ("w"): ao fechar, vira um som da biblioteca
// Só nomes que já estão na lista são lidos ou apagados: nada de caminho vindo de fora.
package com.implacavel.alarme

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileNotFoundException

class FonteDeSons : ContentProvider() {
    override fun onCreate(): Boolean = true

    /** A biblioteca pronta (a fonte pode ser chamada enquanto o app ainda está subindo). */
    private fun pronta() {
        Sons.carregar(context!!)
        Sons.reler()
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, ordem: String?): Cursor {
        pronta()
        return MatrixCursor(arrayOf("nome", "tamanho")).apply {
            Sons.lista.value.forEach { addRow(arrayOf<Any>(it.nome, it.arquivo.length())) }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        pronta()
        val (tipo, nome) = partes(uri) ?: throw FileNotFoundException(uri.toString())
        return when {
            tipo == "som" && mode == "r" -> {
                val som = Sons.achar(nome) ?: throw FileNotFoundException(nome)
                ParcelFileDescriptor.open(som.arquivo, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            tipo == "novo" && mode.startsWith("w") -> {
                val temporario = Sons.temporario(context!!)
                // Quando o Maverick fecha o arquivo: deu certo, entra na biblioteca; deu erro, vai fora
                ParcelFileDescriptor.open(
                    temporario,
                    ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE,
                    Handler(Looper.getMainLooper()),
                ) { erro ->
                    if (erro == null) {
                        Sons.receber(temporario, nome)
                    } else {
                        Log.w(TAG, "Sons: o Maverick não terminou de mandar $nome", erro)
                        temporario.delete()
                    }
                }
            }
            else -> throw FileNotFoundException("$tipo/$nome ($mode)")
        }
    }

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int {
        pronta()
        val (tipo, nome) = partes(uri) ?: return 0
        if (tipo != "som" || Sons.achar(nome) == null) return 0
        Sons.remover(nome)
        return 1
    }

    /** "som"/"novo" e o nome, de content://…/som/NOME. */
    private fun partes(uri: Uri): Pair<String, String>? =
        uri.pathSegments.takeIf { it.size == 2 }?.let { it[0] to it[1] }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int = 0
}
