# Análise: o celular desligou e o alarme do Chiron não tocou (13/11/2026)

Aparelhos: Samsung S24 FE (Android 16, One UI 8) e Galaxy Watch7 (Wear OS).
Regras da solução: o Nubank não pode bloquear o celular por causa dela, nada pago, nada de root, e simples.

## 1. O que aconteceu hoje

**Causa provável:** na hora do alarme do Chiron, o celular estava desligado, e nenhum outro aparelho sabia do alarme.
O Android não liga o celular para tocar um alarme. O relógio só tocava quando o celular mandava.

Confirmado pelos logs (`batterystats`, `dropbox`, `getprop`, `logcat`):

| Hora | O que houve |
|---|---|
| 04:45 | O Turbilhão tocou e a missão começou (STOP, conversa e depois a caminhada com GPS). |
| 05:03:27 | **Desligamento limpo, por software**, no meio da caminhada. Motivo gravado pelo sistema: `shutdown,userrequested`. Bateria a 36%, sem calor. |
| ~05:54 | O dono religou pelo botão (sem carregador). |
| 05:55:07 | O Turbilhão voltou a tocar sozinho no boot. Essa defesa funcionou. |
| 06:00:05 | **Segundo desligamento limpo.** O motivo dele se perdeu: o boot seguinte sobrescreveu o registro. |
| 06:00–08:07 | **Celular desligado.** Nada tocou: nem o Chiron, nem o relógio. |
| ~08:07 | Cabo ligado. O celular entrou no modo de carga e depois deu boot. |
| 08:08:14 | O Turbilhão voltou de novo. Às 08:08:15 ele foi parado pelo cabo (`PararPeloCabo`), a pedido do dono. |
| 08:08:15 | O Chiron tocou o alarme perdido, **atrasado**: a regra dos atrasados (até 24 h) armou o toque no boot. |

O que é suposição (não está nos logs):

- **05:03, pelo menu de energia.** O dono estava com o celular na mão, na caminhada. O motivo `userrequested` só diz que foi uma pessoa, não por qual caminho.
- **06:00, pelo Bixby por voz.** O `com.samsung.android.bixby.wakeup` estava em primeiro plano 24 s antes, e o dono contou que "mandou desligar".
- **A hora original do alarme do Chiron** não está nos logs. Ela ficou entre 05:03 e 08:07.

Não foi reinício forçado, bateria acabando, temperatura nem economia de bateria. Os dois boots vieram do botão e do carregador.

Achados de apoio:

- **O Turbilhão não bloqueia nenhum caminho de desligar.** A única defesa é voltar a tocar quando o celular religa (`App.onCreate`). Ela funcionou nas duas vezes, mas não segura quem deixa o celular desligado.
- **O `ShutdownBlocker` do Alarmy** (acessibilidade, instalado pela Play) estava ligado e não segurou nenhum dos dois desligamentos. O provável é que ele só aja com um alarme do próprio Alarmy tocando (a confirmar).
- **O Chiron espera o Turbilhão.** Com a missão em andamento, o Chiron adia o próprio alarme em 10 min, de novo e de novo (`Avisos.esperarOTourbillon`). Hoje isso não pesou: às 08:08 o Turbilhão foi parado pelo cabo antes de o Chiron tocar.

## 2. Opções pesquisadas

| Opção | Funciona? | Custa | Root | Risco Nubank |
|---|---|---|---|---|
| Voltar a tocar no boot (já existe) | Só depois de religar. Não segura quem deixa desligado | Grátis | Não | Nenhum |
| Acessibilidade que fecha o menu de energia (o nosso `VigiaDoDesligar`, removido em 28/10) | **Não**: o Voltar fechou o menu 0 de 5 vezes no One UI 8. E não existe ação global que feche o menu | Grátis | Não | **Alto, medido**: o Nubank bloqueou o dono, até num teste de 30 s |
| Device Owner por `adb shell dpm set-device-owner` | Sozinho não bloqueia nada. Ele só libera as opções abaixo | Grátis | Não | Desconhecido (qualquer app vê os admins ativos). Exige tirar **todas** as contas antes, e só sai com reset de fábrica |
| Device Owner + lock task sem `LOCK_TASK_FEATURE_GLOBAL_ACTIONS` | Sim, mas **só enquanto a tela está fixada**. O celular vira quiosque à noite | Grátis | Não | Desconhecido (é o mesmo Device Owner) |
| Restrições `DISALLOW_*` (Device Owner) | Nenhuma trata de desligar. `DISALLOW_SAFE_BOOT` só fecha o modo de segurança | Grátis | Não | Desconhecido |
| Fixar tela sem Device Owner | Não: o usuário sai sozinho | Grátis | Não | Nenhum |
| **Knox `RestrictionPolicy.allowPowerOff(false)`** (app como administrador do aparelho + licença KPE) | **Provável pela doc**: bloqueia a tecla, o painel rápido e o reset de fábrica, e vale desde o boot. Não está na lista do que o Android 15+ tira do modo administrador. **Nunca testado no One UI 8** | Grátis: KPE Premium grátis, 2 anos por ativação. Pede conta no portal Knox | Não | **Desconhecido**: o administrador ativo é visível a qualquer app |
| Knox Manage / Knox Suite (MDM da Samsung) | Sim | **Pago**: US$ 30 a 52,50 por aparelho por ano | Não | Desconhecido |
| "Bloquear rede e segurança" (One UI) | Parcial: com a tela bloqueada, desligar pede o PIN. Desbloqueado, não pede nada. É atrito, não trava | Grátis | Não | Nenhum |
| Desligar o despertar por voz do Bixby | Fecha um caminho (o provável das 06:00) | Grátis | Não | Nenhum |
| `ShutdownBlocker` do Alarmy (app da Play) | Não segurou hoje | Grátis | Não | Convive hoje com o Nubank |
| Certificado CA instalado no celular | **Não ajuda em nada** contra desligar. É o caminho errado e só deixa o aviso "a rede pode ser monitorada" | Grátis | Não | Baixo a médio (é sinal de interceptação) |
| Certificado de assinatura pago ou conta paga na Play | Desnecessário: o APK com a nossa chave já instala pelo cabo | Pago | Não | Nenhum |
| Root | Funcionaria | Grátis | **Sim** | **Alto** (o banco recusa o aparelho) |
| Alarme que liga o celular desligado | **Não existe** no S24 FE, nem por API pública | — | — | — |
| M4 (servidor) | Não faz barulho no quarto e não liga o celular | Grátis | Não | Nenhum |
| **Plano B no relógio ("espelho")** | **Sim**: o relógio guarda a agenda e toca sozinho quando o celular some. Cobre todos os casos impossíveis da seção 3. Exige o relógio no pulso e carregado | Grátis | Não | **Nenhum**: nada muda no celular além de mensagens Bluetooth entre apps nossos |

## 3. O que é impossível sem root

- **Bloquear 100% dos jeitos de desligar.** Nenhuma API pública cancela o desligamento. O `ACTION_SHUTDOWN` só avisa que ele vai acontecer. Até o Knox, se funcionar, deixa os três casos abaixo.
- **Reinício forçado (tecla lateral + volume baixo por 7 s).** É função do hardware, sem aviso ao Android. Ele reinicia e não desliga: o Turbilhão e o Chiron voltam no boot.
- **"Power off" do modo de recuperação e o modo download.** Ficam antes do Android.
- **Bateria a 0% ou desligamento por temperatura.**
- **Tocar com o celular desligado.** A doc do `AlarmManager` diz que os alarmes "will be cleared if it is turned off and rebooted". A Samsung confirma que o alarme do S24 FE só toca com o aparelho ligado.

Mitigação de todos: o Plano B no relógio. Para a bateria, também o celular no carregador à noite.

## 4. A solução escolhida

**O celular desligar não pode ser impedido sem root. Então desligar o celular deixa de adiantar: o relógio toca.**

1. **Plano B no relógio, o "espelho" (código).**
   - O Chiron manda ao relógio, por Bluetooth, a agenda dos próximos toques. Ele manda a cada mudança, a cada sincronia de 15 min e no boot.
   - O Turbilhão conta ao Chiron o próximo despertador e, durante a missão, um "ainda estou vivo" a cada minuto.
   - O relógio guarda a última agenda e arma no alarme dele o próximo toque. Na hora:
     - **celular perto:** o relógio não toca pelo espelho, porque o celular toca (e o eco de hoje continua);
     - **celular desligado ou longe:** o relógio toca sozinho, pelo mesmo caminho de alarme que já existe.
   - Na missão, se o celular sumir, o relógio toca em até 5 min: "a missão parou".

   Por quê:
   - É a única opção que faz o alarme tocar em **todos** os casos da seção 3.
   - Não acrescenta nenhum sinal que o Nubank veja: sem acessibilidade, sem sobreposição nova, sem administrador, sem root.
   - É grátis e pequeno: cerca de 90 linhas no Veyron, 30 no Chiron e 20 no Turbilhão.
   - Reaproveita o alarme do Veyron, que já foi testado.

2. **Passos do dono, sem risco** (na lista no fim): desligar o despertar por voz do Bixby, ligar "Bloquear rede e segurança", tirar o Chiron da economia de bateria, dormir com o relógio no pulso e carregado e com o celular no carregador.

3. **O que fica de fora, e por quê:**
   - **Knox `allowPowerOff`.** É o único bloqueio real sem root, mas não entra agora, por quatro motivos:
     - o risco no Nubank é desconhecido, e testar pode custar um bloqueio, que é justamente a regra do dono;
     - depende de conta e chave Knox que só o dono pode criar;
     - nunca foi testado no One UI 8;
     - a chave e o `knoxsdk.jar` não podem ir para este repositório público.

     Se o dono decidir tentar, são cerca de 40 linhas: um `DeviceAdminReceiver` e a chamada ao Knox por reflexão, sem o jar. Isso só depois do espelho pronto, e num dia em que ele aceite o risco no Nubank.
   - **Device Owner com lock task.** Custa tirar todas as contas, deixa o celular em quiosque à noite, o risco no Nubank é desconhecido e só sai com reset de fábrica.
   - **A acessibilidade.** Já falhou no One UI 8, e o Nubank bloqueou.
   - **O Chiron esperar o Turbilhão** continua como está. Enquanto o Turbilhão toca, o dono está sendo acordado. Se o celular sumir, a hora adiada já vai na agenda do relógio.

## 5. O Nubank

**Situação:**
- Hoje o Nubank funciona com a depuração USB ligada e com dois serviços de acessibilidade instalados pela Play (Alarmy e Stay Focused).
- O que o bloqueou, medido em outubro, foi a acessibilidade de um app instalado fora da Play: o nosso `VigiaDoDesligar`.
- O Nubank não publica quais sinais olha. Root tem relato público de bloqueio. Para administrador do aparelho e Device Owner não há fonte nenhuma.

**A solução escolhida não muda nada para o Nubank:**
- não acrescenta acessibilidade, sobreposição, administrador, Device Owner nem root;
- o Turbilhão continua sem nenhum deles.

**Canais oficiais, se um dia o app neste celular parar:**
- **O app em outro aparelho.** Um Android antigo, de fábrica, sem root e sem Device Owner, autorizado pelo próprio app. É o caminho recomendado se o dono adotar o Knox ou o Device Owner um dia.
- **O site para pessoa física não existe mais** desde setembro de 2024. Só a conta PJ tem acesso pelo navegador.
- **Só para emergência:** a página "Me Roubaram" (`app.nubank.com.br/roubo/`), que bloqueia os cartões e desconecta os aparelhos, e o telefone 24h (4020 0185 nas capitais, 0800 591 2117 nas demais).
- **O cartão físico** funciona sem o app.

**O que não fazemos (a linha):**
- WebView nossa com o login do banco;
- esconder do app do banco root, Device Owner, depuração, acessibilidade ou qualquer outro sinal;
- perfil de trabalho ou segundo usuário para escapar da detecção;
- APIs ou apps não oficiais;
- passar a senha do banco a qualquer automação.

Isso é contornar a segurança do banco, e os termos de uso do Nubank (seções 3, 8 e 9) permitem encerrar a conta por isso.

## 6. O que será implementado

Uma tarefa é um commit. Antes de cada commit, rodar os testes JVM do repositório (`./gradlew testDebugUnitTest assembleRelease`).

**Veyron (relógio)**
- V1. O contrato da agenda: `/veyron/v1/espelho/agenda`, `lerAgenda` e os testes.
- V2. `Alarmes.naFila` e o +10 sem celular, que passa a tocar de novo no relógio em 10 min.
- V3. `espelho/Espelho.kt`: guarda a agenda, arma o próximo toque e, na hora, toca só se o celular não estiver perto. Mais o receptor do boot e o `TesteDoEspelho` (só pelo cabo, sempre silencioso).
- V4. `docs/ARCHITECTURE.md` atualizado.

**Chiron**
- C1. `Veyron.agendaJson` e `enviarAgenda`, chamados no fim de `Agendador.reagendar`.
- C2. `DespertadorReceiver`: recebe do Turbilhão o despertador e a missão e junta tudo na agenda.

**Turbilhão**
- T1. Este documento.
- T2. Avisar o Chiron do próximo despertador (`Agendador.agendar` e `cancelar`) e da missão viva (`agendarRetomada`, no máximo 1 vez por minuto, e `cancelarRetomada`).
- T3. O README e o `PENDENCIAS.md` apontam para este documento.

**No aparelho**, pelo cabo:
- antes de instalar, guardar os APKs atuais e conferir que nenhum alarme está tocando;
- testes só silenciosos;
- no celular, só `logcat`, `dumpsys` e receptores de teste.

O "celular longe" se simula desligando o Bluetooth **do relógio** por alguns minutos; ele é religado no fim.

### Testes

Rodados em 13/11/2026, entre 08:57 e 09:01, pelo cabo. **O relógio não apareceu no `adb devices`** (`adb mdns services` vazio). O PC está no hotspot do celular (10.121.20.x). Por isso só a parte do celular foi testada. Os passos do relógio **não rodaram** e nada foi mexido nele (o Bluetooth dele continua como estava).

**Testes JVM** (`./gradlew --no-build-cache cleanTestDebugUnitTest testDebugUnitTest assembleRelease`, com o `flock`; os três passaram):

| Repo | Testes | Resultado |
|---|---|---|
| Veyron | 286 em 18 classes | passou (0 falhas, 0 erros) |
| Chiron | 329 em 25 classes | passou (0 falhas, 0 erros, 1 ignorado) |
| Turbilhão | 114 em 10 classes | passou (0 falhas, 0 erros) |

Os APK release do Chiron e do Turbilhão têm a mesma chave (`Android Debug`, SHA-256 `b2bc0299…`) dos instalados no celular, então o `install -r` mantém os dados. O do Veyron tem essa mesma chave; a do Veyron que está no relógio não foi conferida.

**No celular (feito):**

| Passo | Resultado | Prova |
|---|---|---|
| 0. Antes | **ok** | `dumpsys alarm`: próximo toque do Chiron hoje 18:30 (a SINCRONIZAR das 09:14 não toca) e do Turbilhão 14/11 04:45, nenhum nos 30 min seguintes. `dumpsys activity services`: sem `AlarmeService` nem `AvisoService`. Backup dos APKs em `backup-apk/2026-11-13/`: `celular-chiron-0857.apk` (3 339 715 bytes) e `celular-turbilhao-0857.apk` (43 321 897 bytes). O Veyron do relógio **não foi guardado** (relógio fora do adb) |
| 2. Chiron (celular) | **ok** | `install -r` deu `Success` (08:59:15). Log do Chiron: `Avisos agendados: 60 futuros, 0 atrasados`. Sem nenhum `Veyron: … não chegou ao relógio`. O toque mais cedo do `LembreteReceiver` é **13/11 18:30:00**: é a hora que o relógio deve mostrar |
| 3. Turbilhão (celular) | **ok** | `install -r` deu `Success`. `dumpsys activity broadcasts`: `MY_PACKAGE_REPLACED` às 09:00:05.831, e às 09:00:06.010 o `io.github.luiz_nast.chiron.routine.DESPERTADOR` vindo de `com.implacavel.alarme`, entregue ao `DespertadorReceiver` do Chiron em 8 ms (a permissão de assinatura `…permission.IA` valeu). `dumpsys alarm com.implacavel.alarme`: o `DISPARAR` de 14/11 04:45 voltou ao lugar depois da troca |

**No relógio (não rodou, falta):**

- 1. Veyron por `./instalar.sh <ip:porta>`. O APK release já está compilado (`Veyron/app/build/outputs/apk/release/app-release.apk`).
- 2. (relógio) O log do Veyron dizer `espelho: N toques na cópia, armado para 18:30`.
- 3. (relógio) O log contar o item `tourbillon-<id>` às 04:45.
- 4. Celular perto: `TesteDoEspelho --ei segundos 20`, esperado `celular perto: quem toca é o eco`.
- 5. Celular longe: Bluetooth do relógio desligado, `TesteDoEspelho`, a cobrança (a, b e c) e religar o Bluetooth.
- Para destravar, o dono liga a depuração por Wi-Fi do relógio na rede do hotspot e roda `adb connect <ip:porta>`; as ordens acima valem como estão.

**Estado agora:** o celular tem o Chiron e o Turbilhão novos e o relógio ainda tem o Veyron antigo. **O plano B não está ativo** até o Veyron novo entrar no relógio. O Chiron já manda a agenda (a cada mudança, a cada sincronia de 15 min e no boot), e o Veyron novo a pega na próxima.

**Sem teste de qualquer jeito:**

- O celular desligado de verdade. Só vira prova no próximo desligamento real.
- A hora de dormir e o Não perturbe do relógio (a lacuna que consta em "Limites que ficam").
- A missão viva: não há disparo silencioso pelo cabo. Fica "a conferir no próximo alarme real, só por log": no relógio, `tourbillon-missao` rearmado a cada minuto para +5 min.

### Limites que ficam

- O relógio desligado, sem bateria ou no modo "Só relógio" não toca.
- A agenda do relógio é a do último envio antes de o celular sumir. Um lembrete criado no site depois disso não chega.
- O relógio guarda o texto dos lembretes.
- "Celular perto" é tratado como "o celular vai tocar". Se o celular estiver perto e o Chiron falhar, ninguém toca, igual a hoje.
- Suposição: antes do primeiro desbloqueio depois de um boot, o Play Services pode não falar com o relógio. Nesse caso vale a última agenda que ele recebeu.
- Falta medir se o alarme do relógio passa no modo hora de dormir e no Não perturbe do relógio.

## Fontes

- [AlarmManager: os alarmes somem quando o aparelho desliga](https://developer.android.com/reference/android/app/AlarmManager)
- [Lock task mode](https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode) e [Cookbook (Device Owner por adb, DISALLOW_SAFE_BOOT)](https://developer.android.com/work/dpc/dedicated-devices/cookbook)
- [AccessibilityService: ações globais](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Knox RestrictionPolicy.allowPowerOff](https://docs.samsungknox.com/devref/knox-sdk/reference/com/samsung/android/knox/restriction/RestrictionPolicy.html)
- [Knox: métodos tirados do modo administrador no Android 15+](https://docs.samsungknox.com/dev/knox-sdk/api-reference/restricted-api-methods/)
- [Knox: licença KPE Premium grátis](https://docs.samsungknox.com/admin/knox-platform-for-enterprise/before-you-begin/knox-platform-for-enterprise-licenses/)
- [Samsung: o alarme não toca com o aparelho desligado](https://www.samsung.com/ae/support/mobile-devices/will-the-alarm-turn-on-in-my-samsung-phone-or-tablet-if-the-device-is-turned-off)
- [Samsung: reinício forçado](https://www.samsung.com/ph/support/mobile-devices/how-to-force-reboot-your-samsung-galaxy-smartphone/)
- [Comunidade Samsung: "Bloquear rede e segurança" pede desbloqueio para desligar](https://eu.community.samsung.com/t5/galaxy-a-series/how-i-turn-on-the-setting-quot-require-password-to-power-off/m-p/12530171)
- [Play Integrity: vereditos de risco de acesso de apps](https://developer.android.com/google/play/integrity/verdicts)
- [Data Layer: sem Bluetooth, os dados vão pela nuvem](https://developer.android.com/training/wearables/data/overview)
- [Nubank: fim do acesso de pessoa física pelo navegador](https://canaltech.com.br/apps/nubank-vai-descontinuar-acesso-a-conta-pessoal-pelo-computador/)
- [Nubank: contatos](https://nubank.com.br/contato/) e [termos de uso](https://nubank.com.br/contrato/termos-de-uso/)
