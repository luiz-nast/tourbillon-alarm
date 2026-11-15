// A chuva de agora no lugar da pessoa, pra decidir a caminhada: chuva fraca não muda nada; chuva FORTE
// oferece trocar a caminhada na rua pela câmera (ele escolhe). A última localização que o celular já conhece (sem ligar
// o GPS) e o tempo atual do Open-Meteo (sem chave). Pro Open-Meteo só vão a latitude e a longitude arredondadas (uns
// 1 km). Sem localização, sem internet ou com erro, não dá pra saber: aí não oferece nada, segue a caminhada.
// Decidir se a chuva é forte e ler a resposta são funções puras (testadas em ClimaTest).
package com.implacavel.alarme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.Locale
import kotlin.concurrent.thread

/** A partir daqui (mm na última hora, somados pelo Open-Meteo) é chuva forte: encharca, melhor a câmera. */
private const val CHUVA_FORTE_MM = 2.5

// Códigos do tempo (WMO) do Open-Meteo: chuva forte, pancada forte, tempestade
private val CODIGOS_FORTES = setOf(65, 67, 82, 95, 96, 99)

/**
 * Chuva forte (oferece trocar a caminhada pela câmera; fraca ou sem dado, segue a rua) por [precipitacaoMm] e pelo
 * [codigo] do tempo (WMO): vale o mais forte dos dois.
 */
fun chuvaForte(precipitacaoMm: Double?, codigo: Int?): Boolean =
    (precipitacaoMm != null && precipitacaoMm >= CHUVA_FORTE_MM) || (codigo != null && codigo in CODIGOS_FORTES)

/** Chuva forte na resposta do Open-Meteo (`current.precipitation` + `current.weather_code`). Quebrada: não. */
fun chuvaForteNoOpenMeteo(json: String): Boolean = runCatching {
    val agora = JSONObject(json).getJSONObject("current")
    chuvaForte((agora.opt("precipitation") as? Number)?.toDouble(), (agora.opt("weather_code") as? Number)?.toInt())
}.getOrDefault(false)

/** O pedido do tempo de agora no Open-Meteo, com o lugar arredondado em 2 casas (uns 1 km), sempre com ponto decimal. */
fun enderecoDoClima(latitude: Double, longitude: Double): String =
    "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=precipitation,weather_code".format(Locale.US, latitude, longitude)

object Clima {
    private val handler = Handler(Looper.getMainLooper())
    private var pedidos = 0 // muda a cada [cancelar]: resposta de uma busca velha não chega

    /** Se a chuva agora é forte no último lugar conhecido, entregue na thread principal ([aoSaber]) em até uns 10 s. */
    fun chuva(ctx: Context, aoSaber: (forte: Boolean) -> Unit) {
        val numero = pedidos
        val lugar = ultimoLugar(ctx)
        if (lugar == null) Log.i(TAG, "Clima: sem localização conhecida, não dá pra saber a chuva")
        thread(isDaemon = true, name = "clima") {
            val forte = lugar?.let { runCatching { baixarTexto(enderecoDoClima(it.latitude, it.longitude))?.let(::chuvaForteNoOpenMeteo) }.getOrNull() } == true
            Log.i(TAG, "Clima: chuva forte=$forte")
            handler.post { if (numero == pedidos) aoSaber(forte) }
        }
    }

    /** A busca em curso não entrega mais nada. */
    fun cancelar() {
        pedidos++
    }

    /**
     * A localização mais nova que o celular já tem (do "fused", da rede ou do GPS), sem ligar nada. Pode ser
     * antiga: pra chuva, basta a cidade. Null sem permissão ou sem nenhuma.
     */
    @SuppressLint("MissingPermission") // conferida logo abaixo
    private fun ultimoLugar(ctx: Context): Location? {
        val permitido = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
        if (!permitido) return null
        val gps = ctx.getSystemService(LocationManager::class.java) ?: return null
        val fontes = runCatching { gps.allProviders }.getOrDefault(emptyList())
        return listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { it in fontes }
            .mapNotNull { runCatching { gps.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }
}
