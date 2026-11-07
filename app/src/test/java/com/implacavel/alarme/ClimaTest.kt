package com.implacavel.alarme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A força da chuva e a regra de oferecer a câmera (Clima.kt), com dados simulados (o dono, 11-07). */
class ClimaTest {
    @Test fun secoNaoOferece() {
        assertEquals(ForcaDaChuva.SECO, forcaDaChuva(0.0, 0))
        assertEquals(ForcaDaChuva.SECO, forcaDaChuva(0.1, 3)) // nublado, garoa de nada
        assertFalse(ofereceCamera(forcaDaChuva(0.0, 1)))
    }

    @Test fun chuvaFracaSegueNaRua() {
        assertEquals(ForcaDaChuva.FRACA, forcaDaChuva(0.4, null)) // 0,4 mm
        assertEquals(ForcaDaChuva.FRACA, forcaDaChuva(null, 61)) // chuva fraca (WMO)
        assertEquals(ForcaDaChuva.FRACA, forcaDaChuva(null, 51)) // garoa
        assertFalse("chuva fraca não oferece câmera", ofereceCamera(ForcaDaChuva.FRACA))
    }

    @Test fun chuvaForteOfereceACamera() {
        assertEquals(ForcaDaChuva.FORTE, forcaDaChuva(3.0, null)) // 3 mm encharca
        assertEquals(ForcaDaChuva.FORTE, forcaDaChuva(null, 65)) // chuva forte (WMO)
        assertEquals(ForcaDaChuva.FORTE, forcaDaChuva(null, 95)) // tempestade
        assertEquals(ForcaDaChuva.FORTE, forcaDaChuva(10.0, 61)) // mm forte ganha do código fraco
        assertTrue("chuva forte oferece a câmera", ofereceCamera(forcaDaChuva(3.0, 65)))
    }

    @Test fun semDadoNaoSeiNaoOferece() {
        assertEquals(ForcaDaChuva.NAO_SEI, forcaDaChuva(null, null))
        assertEquals(ForcaDaChuva.NAO_SEI, chuvaDoOpenMeteo("não é json"))
        assertFalse("sem saber, segue a caminhada normal", ofereceCamera(ForcaDaChuva.NAO_SEI))
    }

    @Test fun leOResponderDoOpenMeteo() {
        assertEquals(ForcaDaChuva.SECO, chuvaDoOpenMeteo("""{"current":{"precipitation":0.0,"weather_code":2}}"""))
        assertEquals(ForcaDaChuva.FRACA, chuvaDoOpenMeteo("""{"current":{"precipitation":0.5,"weather_code":61}}"""))
        assertEquals(ForcaDaChuva.FORTE, chuvaDoOpenMeteo("""{"current":{"precipitation":4.2,"weather_code":65}}"""))
        assertEquals(ForcaDaChuva.NAO_SEI, chuvaDoOpenMeteo("""{"current":{}}"""))
    }

    @Test fun oEnderecoUsaPontoDecimalEDuasCasas() {
        val url = enderecoDoClima(-15.793, -47.882)
        assertTrue(url, url.contains("latitude=-15.79") && url.contains("longitude=-47.88"))
        assertTrue(url, url.contains("current=precipitation,weather_code"))
    }
}
