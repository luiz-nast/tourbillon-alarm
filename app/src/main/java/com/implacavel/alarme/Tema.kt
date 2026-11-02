// Compartilhado pelas telas: temas (Claro no alarme, Noite na tela principal), cores fixas da tela do alarme, o relógio agoraACada e lidoAoVoltar.
package com.implacavel.alarme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** Cores da tela do alarme, que não segue o tema do sistema. O vermelho é o mesmo de R.color.vermelho_alarme. */
val VermelhoAlarme = Color(0xFF8F2E1D)
val VinhoAlarme = Color(0xFF2A0D08)

/** A etapa da caminhada (as ondas do prazo pra sair e a rua) é azul, não vermelha: pedido do dono (10-17). */
val AzulCaminhada = Color(0xFF0D47A1)
val FundoCaminhada = listOf(Color(0xFFF3F9FF), Color(0xFFBBDEFB))

private val Claro = lightColorScheme(
    primary = Color(0xFF8F2E1D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF3A0E06),
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC),
)

/**
 * A noite do Tourbillon Alarm (a tela principal e o editor, 10-17, o dono pediu componentes escuros), agora na paleta
 * única dos apps do dono (11-02): fundo quase preto azulado, cartões em cinza-azulado, ferrugem no que acende e chama
 * (botões, interruptor ligado, o tipo do alarme; ferrugem clara nos textos e traços sobre o escuro), cinzas no resto e
 * vermelho claro no erro. Nunca segue o sistema: é sempre esta.
 */
private val Noite = darkColorScheme(
    primary = Color(0xFFC4472E),
    onPrimary = Color(0xFFECEEF0),
    primaryContainer = Color(0xFF8F2E1D),
    onPrimaryContainer = Color(0xFFECEEF0),
    secondary = Color(0xFF949AA0),
    onSecondary = Color(0xFF0B0E12),
    secondaryContainer = Color(0xFF27313A),
    onSecondaryContainer = Color(0xFFECEEF0),
    tertiary = Color(0xFFB8BEC4),
    onTertiary = Color(0xFF0B0E12),
    tertiaryContainer = Color(0xFF4E5158),
    onTertiaryContainer = Color(0xFFECEEF0),
    error = Color(0xFFE0654A),
    onError = Color(0xFF0B0E12),
    errorContainer = Color(0xFF5A1F14),
    onErrorContainer = Color(0xFFFFDAD3),
    background = Color(0xFF0B0E12),
    onBackground = Color(0xFFECEEF0),
    surface = Color(0xFF0B0E12),
    onSurface = Color(0xFFECEEF0),
    surfaceVariant = Color(0xFF27313A),
    onSurfaceVariant = Color(0xFF949AA0),
    outline = Color(0xFF7B848C),
    outlineVariant = Color(0xFF4E5158),
    surfaceContainerLowest = Color(0xFF070A0D),
    surfaceContainerLow = Color(0xFF141A20),
    surfaceContainer = Color(0xFF1B232A),
    surfaceContainerHigh = Color(0xFF222B33),
    surfaceContainerHighest = Color(0xFF27313A),
)

/**
 * Tema do app, que nunca segue o modo claro/escuro do sistema (o dono pediu). A tela do alarme fica com o [Claro]
 * (tons de vermelho; o YouTube da etapa da câmera, um WebView, segue o tema claro da tela e fica claro, pra iluminar o
 * rosto); a tela principal e o editor, com [escuro], ficam na [Noite].
 */
@Composable
fun TemaImplacavel(escuro: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (escuro) Noite else Claro, content = content)
}

/** Hora atual, que se atualiza sozinha a cada [intervaloMs] (relógio da tela do alarme, textos "toca em 9 h"). */
@Composable
fun agoraACada(intervaloMs: Long): ZonedDateTime {
    var agora by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(intervaloMs) {
        while (true) {
            delay(intervaloMs)
            agora = ZonedDateTime.now()
        }
    }
    return agora
}

/**
 * O que [ler] devolve, lido de novo toda vez que a tela volta pro primeiro plano (depois do diálogo de
 * permissão do sistema ou de voltar das Configurações) e quando [chave] muda: permissões da tela
 * principal, câmera do alarme.
 */
@Composable
fun <T> lidoAoVoltar(chave: Any? = Unit, ler: () -> T): T {
    val lerAgora by rememberUpdatedState(ler)
    var valor by remember { mutableStateOf(ler()) }
    LifecycleResumeEffect(chave) {
        valor = lerAgora()
        onPauseOrDispose { }
    }
    return valor
}
