package com.implacavel.alarme

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Os comandos do cabo, e a chamada ao Knox por reflexão nunca derruba o app (TravaDoDesligar.kt). */
class TravaDoDesligarTest {
    @Test
    fun travarNaoDeixaDesligar() = assertEquals(false, TravaDoDesligar.permitir("travar"))

    @Test
    fun liberarDeixaDesligar() = assertEquals(true, TravaDoDesligar.permitir("liberar"))

    @Test
    fun sairLiberaAntes() = assertEquals(true, TravaDoDesligar.permitir("sair"))

    @Test
    fun semComandoOuDesconhecidoNaoMexe() {
        assertNull(TravaDoDesligar.permitir(null))
        assertNull(TravaDoDesligar.permitir("TRAVAR"))
        assertNull(TravaDoDesligar.permitir(""))
    }

    @Test
    fun semOKnoxNaoLanca() {
        // Fora da Samsung (e aqui, no teste) as classes do Knox não existem
        val r = TravaDoDesligar.tentar { Class.forName("com.samsung.android.knox.EnterpriseDeviceManager") }
        assertTrue(r.exceptionOrNull() is ClassNotFoundException)
    }

    @Test
    fun oErroDeDentroVemDesembrulhado() {
        val r = TravaDoDesligar.tentar { throw InvocationTargetException(SecurityException("sem licença")) }
        assertTrue(r.exceptionOrNull() is SecurityException)
    }

    @Test
    fun erroGraveTambemNaoLanca() {
        val r = TravaDoDesligar.tentar { throw NoClassDefFoundError("knox") }
        assertTrue(r.exceptionOrNull() is NoClassDefFoundError)
    }

    @Test
    fun oValorPassa() = assertEquals(true, TravaDoDesligar.tentar { true }.getOrNull())
}
