// Administrador do aparelho, sem política nenhuma: é só o que o Knox exige pra travar o desligar (TravaDoDesligar.kt).
// Ativar e desativar é passo do dono, pelo cabo ou em Configurações → Segurança → Apps de administração:
//   adb shell dpm set-active-admin com.implacavel.alarme/.AdminDoAlarme
//   adb shell dpm remove-active-admin com.implacavel.alarme/.AdminDoAlarme
// Veja docs/analise-desligamento.md.
package com.implacavel.alarme

import android.app.admin.DeviceAdminReceiver

class AdminDoAlarme : DeviceAdminReceiver()
