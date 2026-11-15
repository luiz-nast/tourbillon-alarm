package com.implacavel.alarme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quando a chuva é forte o bastante pra oferecer a câmera (Clima.kt), com dados simulados (o dono, 11-07). */
class ClimaTest {
    @Test fun secoOuFracaSegueNaRua() {
        assertFalse(chuvaForte(0.0, 0))
        assertFalse(chuvaForte(0.1, 3)) // nublado, garoa de nada
        assertFalse(chuvaForte(0.4, null)) // 0,4 mm
        assertFalse(chuvaForte(null, 61)) // chuva fraca (WMO)
        assertFalse(chuvaForte(null, 51)) // garoa
    }

    @Test fun chuvaForteOfereceACamera() {
        assertTrue(chuvaForte(3.0, null)) // 3 mm encharca
        assertTrue(chuvaForte(null, 65)) // chuva forte (WMO)
        assertTrue(chuvaForte(null, 95)) // tempestade
        assertTrue(chuvaForte(10.0, 61)) // mm forte ganha do código fraco
        assertTrue(chuvaForte(3.0, 65))
    }

    @Test fun semDadoSegueNaRua() {
        assertFalse(chuvaForte(null, null))
        assertFalse(chuvaForteNoOpenMeteo("não é json"))
    }

    @Test fun leOResponderDoOpenMeteo() {
        assertFalse(chuvaForteNoOpenMeteo("""{"current":{"precipitation":0.0,"weather_code":2}}"""))
        assertFalse(chuvaForteNoOpenMeteo("""{"current":{"precipitation":0.5,"weather_code":61}}"""))
        assertTrue(chuvaForteNoOpenMeteo("""{"current":{"precipitation":4.2,"weather_code":65}}"""))
        assertFalse(chuvaForteNoOpenMeteo("""{"current":{}}"""))
    }

    @Test fun oEnderecoUsaPontoDecimalEDuasCasas() {
        val url = enderecoDoClima(-15.793, -47.882)
        assertTrue(url, url.contains("latitude=-15.79") && url.contains("longitude=-47.88"))
        assertTrue(url, url.contains("current=precipitation,weather_code"))
    }
}
