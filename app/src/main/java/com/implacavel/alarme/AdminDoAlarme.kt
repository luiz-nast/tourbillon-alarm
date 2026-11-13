// Administrador do aparelho, sem política nenhuma: é só o que o Knox exige pra travar o desligar (TravaDoDesligar.kt).
// Ativar e desativar é passo do dono. Ativar, pelo cabo:
//   adb shell dpm set-active-admin com.implacavel.alarme/.AdminDoAlarme
// Desativar, em Configurações → Segurança e privacidade → Apps de administração do dispositivo, ou pelo cabo com o
// `--es comando sair` do TravaPeloCabo (o `dpm remove-active-admin` só remove admin de teste, android:testOnly).
// Veja docs/analise-desligamento.md.
package com.implacavel.alarme

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class AdminDoAlarme : DeviceAdminReceiver() {
    // Atrito, não trava: o aviso aparece antes de desativar em Configurações
    override fun onDisableRequested(ctx: Context, intent: Intent) = "Isso destrava o desligar. Travar de novo, só pelo cabo."
}
