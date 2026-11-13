# Relatório: Knox fora e o relógio implacável (13/11/2026)

## Resumo

| Frente | Estado |
|---|---|
| 1. Tirar o Knox | **Código pronto, mas não aplicado.** O celular não apareceu no adb (das 12:01 em diante), então a saída pelo cabo (`sair`) não rodou. Pela regra da missão, o código não sai da `main` com o administrador ativo. A remoção está no branch `knox-fora`, testada e no GitHub. |
| 2. Relógio implacável | **Feito e no GitHub** (Veyron, Chiron e Turbilhão). |
| 3. Instalar e testar no relógio | **O relógio real não apareceu no adb.** Plano B: 7 testes no relógio virtual, todos passaram. Os 3 testes ao vivo pedidos não rodaram. |

**Commits:**
- 6 commits na `main`, todos com push: Veyron `076d952`, `1aabbed` e `a4e162d`; Chiron `8d59e4a`; Turbilhão `ab67854` e o deste relatório.
- 2 commits no branch `knox-fora` do Turbilhão, com push: `88d058a` (tira o código) e `2e4c854` (docs).

## O que foi feito

- **Veyron, o relógio implacável** (`Veyron/docs/RELOGIO_IMPLACAVEL.md`):
  - **Quando toca:** no despertador do Turbilhão com o celular sumido, ou quando o sinal de vida da missão para de chegar.
  - **Como toca:** sem parar, sem botão e sem o corte dos 45 s. Ele volta a cada 60 s se o serviço morrer, e volta depois de um reboot, inclusive antes do primeiro desbloqueio.
  - **O que o cala:** só uma batida nova do Turbilhão ou a missão concluída, que o Chiron conta. O `parar` do celular é ignorado para o Turbilhão.
  - **Com o celular perto:** o relógio toca uma vez junto com o despertador, e a tela fica até a missão acabar.
  - **Lembretes comuns:** iguais a antes.
- **Chiron:** a agenda leva ao relógio o estado do Turbilhão (última batida, prazo, missão concluída). O despertador vai sem as 5 cobranças antigas.
- **Turbilhão:** se o celular estava desligado na hora do despertador, ele toca o despertador perdido quando o celular volta (até 12 h). É essa batida que cala o relógio.
- **Knox** (branch `knox-fora`): saem o administrador, a licença, os comandos `travar`, `liberar` e `sair`, o `device_admin.xml` e os receivers. A doc registra por que foi descartado (a chave KPE só sai para empresa aprovada).

## A prova de que o administrador saiu

**Ainda não existe.** O celular está fora do adb. O último estado visto, às 10:58, foi o administrador ativo, nada travado e `powerOffAllowed: true`.

Quando o celular voltar ao cabo, nesta ordem:
1. Rodar o `TravaPeloCabo --es comando sair` com o Turbilhão que já está instalado.
2. Conferir no `dumpsys device_policy` que o `AdminDoAlarme` sumiu.
3. Juntar o `knox-fora` à `main`, rodar os testes e dar push.
4. Instalar o Turbilhão e o Chiron novos e conferir o despertador.

## Testes JVM (antes × depois)

| App | Antes | Depois | Falhas |
|---|---|---|---|
| Veyron | 287 | 315 | 0 |
| Chiron | 330 (1 ignorado) | 335 (1 ignorado) | 0 |
| Turbilhão, `main` | 122 | 129 | 0 |
| Turbilhão, `knox-fora` | — | 121: saem os 8 testes do Knox junto com o código | 0 |

Nenhum teste se perdeu fora isso. No Chiron, o teste das 5 cobranças virou o teste "o despertador vai só com a vez 0".

O `lintDebug` do Turbilhão falha com 14 erros `NewApi` antigos, em arquivos que esta missão não tocou (`AlarmeService`, `Sirene` e `TesteDoSom`). Ficou como tarefa separada.

## Testes no relógio virtual (Wear OS, API 35, sem áudio)

| # | Hora | Teste | Resultado | Trecho do log |
|---|---|---|---|---|
| 1 | 12:24:29 | Despertador do Turbilhão sem celular perto | **ok**: toca implacável, a vibração se repete sem fim (`usage: ALARM`) e toca de novo depois de 60 s | `implacável: toca tourbillon-teste (IMPLACAVEL, …)` / 12:25:29 `tourbillon-teste toca de novo` |
| 2 | 12:26:05 | Sinal velho (batida igual, missão concluída antes) | **ok**: não cala | serviço continua de pé |
| 2b | 12:26:08 | Batida nova do Turbilhão | **ok**: cala; serviço 0, vibração nula, nada pendente | `o Turbilhão voltou: o relógio para` |
| 3 | 12:27:17 | O prazo da missão vence sem batida | **ok**: toca implacável | `implacável: toca tourbillon-missao (IMPLACAVEL, …)` |
| 4 | 12:27:36 | Reboot com o toque ativo | **ok**: volta a tocar depois do boot (nos dois avisos de boot) | 12:27:56 `implacável: toca de novo depois do boot (tourbillon-missao, IMPLACAVEL)` |
| 5 | 12:29:31 e 12:29:52 | Não perturbe em "prioridade" e em "só alarmes" | **ok**: toca, com a vibração repetida de alarme. O Não perturbe foi restaurado | `implacável: toca tourbillon-dnd` |
| 6 | 12:30:33 | A tela | **ok**: no Turbilhão, sem botão ("Só o Turbilhão desliga"); no lembrete comum, "+10 min" e "FEITO" e a vibração única de 1,5 s, como antes | `celular longe: toca` |
| 7 | 12:32 | O voltar e o deslize | **ok**: a `TelaDoAlarme` continua na frente | `topResumedActivity=…TelaDoAlarme` |

**O relógio virtual não prova:**
- o modo "celular perto" (`ACOMPANHA`), porque não há celular pareado;
- a cadeia real Turbilhão → Chiron → relógio;
- a Hora de dormir;
- o boot de um relógio com PIN;
- o som: o relógio virtual está sem áudio, e os testes foram silenciosos.

## Testes ao vivo pedidos (não rodaram)

O celular e o relógio estavam fora do adb. O PC estava na rede 10.13.x e o relógio não apareceu no mDNS.

Para fazer quando os dois voltarem:
1. **Celular perto:** um despertador comum do Turbilhão para dali a 3 min, no volume mínimo, criado no app (o botão "Testar" não manda o sinal de vida). O relógio toca junto, e a missão concluída fecha os dois.
2. **Celular sumido:** com a missão em andamento, desligar o Bluetooth do celular. Em até 5 min o relógio toca sem parar.
3. **Sinal de volta:** religar o Bluetooth com a missão em andamento. Em até 1 min o relógio para.

## O Veyron no relógio

**Não entrou.** O APK está pronto (`Veyron/app/build/outputs/apk/release/app-release.apk`).

O que falta do teu lado:
1. Põe o relógio e o PC na mesma rede Wi-Fi.
2. No relógio, deixa a "Depuração por Wi-Fi" ligada em Opções do desenvolvedor.

A partir daí, eu conecto, instalo e testo.

**Atenção:** até os três apps novos entrarem nos aparelhos, nada disto protege. O celular tem o Turbilhão `6f916b4` e o Chiron `c7d9ed8`, e o relógio tem o Veyron antigo, sem o espelho.

## O despertador de 14/11 às 04:45

- **Visto armado pela última vez às 10:58** (`dumpsys alarm`: `DISPARAR` de 2026-11-14 04:45:00).
- Nada foi instalado no celular desde então, porque ele saiu do adb. Não foi possível conferir de novo.
- Nenhum alarme de teste ficou pendente no celular. No relógio virtual, todos os testes terminaram calados.

## Dois lembretes para ti

1. **Desligar o "Ouvir Oi Bixby"** em Configurações › Recursos avançados › Bixby. O desligamento das 06:00 de 13/11 foi provavelmente por voz.
2. **Ligar "Bloquear rede e segurança"** em Configurações › Tela de bloqueio e AOD › Configurações de bloqueio seguro. Com a tela bloqueada, desligar passa a pedir o PIN.
