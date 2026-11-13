package com.implacavel.alarme

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A chamada ao Knox por reflexão nunca derruba o app (TravaDoDesligar.kt). */
class TravaDoDesligarTest {
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
