// A biblioteca de sons do app (pedido do dono, 10-17): todo som escolhido, pela tela principal ou pelo editor de um
// alarme, é COPIADO pra dentro do app, e fica: o dia em que o arquivo sair da pasta Downloads, o app já tem a versão
// dele. Cada alarme escolhe o seu ([Alarme.som], o nome do arquivo na biblioteca); sem escolha, vale o padrão
// ([Sons.padrao]); sem padrão, o toque de alarme do celular (Sirene). A pasta é protegida (como os alarmes): dá pra
// ler antes do primeiro desbloqueio, então depois de um reinício a música toca, e não o toque do sistema.
// O app não traz música nenhuma: a biblioteca começa vazia. O Mistral usa a mesma biblioteca (FonteDeSons): toca
// os sons na tela de bloqueio dele e também adiciona e apaga (o dono, 10-31).
//
// Pelo cabo, no app de depuração (veja o README):
//   adb push som.mp3 /data/local/tmp/ && adb shell run-as com.implacavel.alarme cp /data/local/tmp/som.mp3 /data/user_de/0/com.implacavel.alarme/app_sons/
package com.implacavel.alarme

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object Sons {
    /** Um som da biblioteca: o [nome] do arquivo (o que a tela mostra e o alarme guarda) e o [arquivo]. */
    data class Som(val nome: String, val arquivo: File)

    private lateinit var pasta: File
    private lateinit var prefs: SharedPreferences
    private val _lista = MutableStateFlow<List<Som>>(emptyList())
    private val _padrao = MutableStateFlow<String?>(null)

    /** Os sons da biblioteca, em ordem de nome. */
    val lista: StateFlow<List<Som>> = _lista.asStateFlow()

    /** O nome do som que toca nos alarmes sem som próprio; null = o toque de alarme do celular. */
    val padrao: StateFlow<String?> = _padrao.asStateFlow()

    /** Chamado em [App.onCreate] e pela [FonteDeSons] (que pode ser chamada antes): só a primeira vez faz algo. */
    @Synchronized
    fun carregar(ctx: Context) {
        if (::pasta.isInitialized) return
        pasta = ctx.createDeviceProtectedStorageContext().getDir("sons", Context.MODE_PRIVATE)
        prefs = ctx.prefsProtegidas("sons")
        // Sobras de um envio do Mistral que o app não terminou de receber (morreu no meio)
        ctx.createDeviceProtectedStorageContext().cacheDir.listFiles { f -> f.name.startsWith("chegando") }?.forEach { it.delete() }
        _padrao.value = prefs.getString("padrao", null)
        reler()
        migrarMusicaAntiga(ctx)
    }

    /** Relê a pasta (um som pode ter chegado pelo cabo); um padrão que sumiu deixa de ser padrão. */
    @Synchronized
    fun reler() {
        _lista.value = (pasta.listFiles() ?: emptyArray())
            .filter { it.isFile && it.length() > 0 }
            .sortedBy { it.name.lowercase() }
            .map { Som(it.name, it) }
        if (_padrao.value != null && existente(_padrao.value!!) == null) escolherPadrao(null)
    }

    /**
     * Copia o áudio em [uri] pra biblioteca ([receber]: com o nome do arquivo, sem repetir, até [TAMANHO_MAXIMO]) e devolve
     * o som; null se não deu pra ler. Espera o que o provedor demorar (um app de nuvem baixa o arquivo na leitura): chame
     * fora da thread da tela.
     */
    fun adicionar(ctx: Context, uri: Uri): Som? {
        val temporario = temporario(ctx)
        val copiou = runCatching {
            ctx.contentResolver.openInputStream(uri)!!.use { entrada -> temporario.outputStream().use { saida -> copiarAte(entrada, saida, TAMANHO_MAXIMO) } }
        }.onFailure { Log.w(TAG, "Sons: não deu pra copiar $uri", it) }.isSuccess
        if (!copiou) {
            temporario.delete()
            return null
        }
        return receber(temporario, nomeDoArquivo(ctx, uri))
    }

    /**
     * Um som que chegou de fora (o Mistral, pela [FonteDeSons], ou o seletor, por [adicionar]), já gravado em [temporario]
     * na mesma partição: vira um som da biblioteca com o [nomeOriginal] (seguro, sem repetir). Vazio ou grande demais, é jogado fora.
     */
    @Synchronized
    fun receber(temporario: File, nomeOriginal: String?): Som? {
        if (temporario.length() <= 0 || temporario.length() > TAMANHO_MAXIMO) {
            temporario.delete()
            return null
        }
        val nome = nomeLivre(nomeSeguro(nomeOriginal), _lista.value.map { it.nome })
        val destino = File(pasta, nome)
        if (!temporario.renameTo(destino)) {
            temporario.delete()
            Log.w(TAG, "Sons: não deu pra guardar o som que chegou: $nome")
            return null
        }
        Log.i(TAG, "Sons: entrou na biblioteca: $nome (${destino.length() / 1024} KB)")
        reler()
        return Som(nome, destino)
    }

    /** O maior som que entra vindo de fora (um arquivo enorme por engano não enche a memória do alarme). */
    private const val TAMANHO_MAXIMO = 100L shl 20

    /** Onde um som de fora é gravado antes de entrar na biblioteca (mesma partição, pra só renomear no fim). */
    fun temporario(ctx: Context): File =
        File.createTempFile("chegando", ".tmp", ctx.createDeviceProtectedStorageContext().cacheDir)

    /** O som [nome], se está na biblioteca (só nomes da lista: nada de caminho vindo de fora). */
    fun achar(nome: String): Som? = existente(nome)

    /** Apaga o som [nome] da biblioteca: os alarmes que o usavam passam a tocar o padrão. */
    @Synchronized
    fun remover(nome: String) {
        File(pasta, nome).delete()
        Log.i(TAG, "Sons: removido da biblioteca: $nome")
        reler()
    }

    fun escolherPadrao(nome: String?) {
        prefs.edit(commit = true) { if (nome == null) remove("padrao") else putString("padrao", nome) }
        _padrao.value = nome
    }

    /**
     * O som que o [alarme] toca, como URI (file://, pra foto do alarme e pro MediaPlayer): o dele, se ainda está na
     * biblioteca; senão o padrão; senão null, e a Sirene toca o toque de alarme do celular.
     */
    fun uriDoAlarme(alarme: Alarme): String? = somDoAlarme(alarme)?.let { Uri.fromFile(it.arquivo).toString() }

    /** O som que o [alarme] toca ([uriDoAlarme]), ou null (o toque do celular). */
    fun somDoAlarme(alarme: Alarme): Som? = alarme.som?.let(::existente) ?: _padrao.value?.let(::existente)

    private fun existente(nome: String): Som? = _lista.value.firstOrNull { it.nome == nome }

    private fun nomeDoArquivo(ctx: Context, uri: Uri): String? = runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    /**
     * A música das versões anteriores (uma URI do seletor do sistema, em Ajustes) vira o padrão da biblioteca: copiada
     * uma vez e esquecida. Se não der pra ler agora (antes do primeiro desbloqueio, por exemplo), fica pra próxima vez.
     */
    private fun migrarMusicaAntiga(ctx: Context) {
        val antigos = ctx.prefsProtegidas("ajustes")
        val uri = antigos.getString("musicaUri", null) ?: return
        val som = adicionar(ctx, uri.toUri()) ?: return
        escolherPadrao(som.nome)
        runCatching { ctx.contentResolver.releasePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        antigos.edit(commit = true) {
            remove("musicaUri")
            remove("musicaNome")
        }
        Log.i(TAG, "Sons: a música de antes virou o padrão da biblioteca: ${som.nome}")
    }
}

/** Um nome de arquivo seguro pra biblioteca: sem barras, sem caracteres de controle, sem "." ou ".." e sem ficar vazio. */
fun nomeSeguro(nome: String?): String {
    val limpo = nome.orEmpty().replace(Regex("[/\\\\\\p{Cntrl}]"), "_").trim()
    return if (limpo.isEmpty() || limpo == "." || limpo == "..") "Sound" else limpo.take(120)
}

/** [nome] sem bater com nenhum dos [existentes]: "Raya.mp3", depois "Raya (2).mp3", "Raya (3).mp3"… */
fun nomeLivre(nome: String, existentes: Collection<String>): String {
    if (nome !in existentes) return nome
    val ponto = nome.lastIndexOf('.').takeIf { it > 0 } ?: nome.length
    val base = nome.substring(0, ponto)
    val extensao = nome.substring(ponto)
    return generateSequence(2) { it + 1 }.map { "$base ($it)$extensao" }.first { it !in existentes }
}

/** Copia [entrada] em [saida] até passar de [limite] bytes (um pouco além já basta: [Sons.receber] recusa) ou acabar. */
internal fun copiarAte(entrada: InputStream, saida: OutputStream, limite: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (total <= limite) {
        val lidos = entrada.read(buffer)
        if (lidos < 0) return
        saida.write(buffer, 0, lidos)
        total += lidos
    }
}
