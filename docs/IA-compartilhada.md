# A IA compartilhada (o lado do Tourbillon)

O Gemma 4 E2B mora só aqui (uma cópia na memória, ~520 MB carregado). Outros apps do dono (hoje, o Chiron Routine)
**perguntam** a ele pelo `ServicoDeIa`. O lado do Chiron está no `docs/IA-compartilhada.md` dele; os dois descrevem o
mesmo contrato.

## O desenho

```
 Chiron  ──Messenger (bindService)──►  ServicoDeIa  ──►  ModeloLocal (o Gemma, thread "gemma")
         ◄──────── RESPOSTA ────────                     a conversa da manhã do Tourbillon usa o mesmo motor
```

- **Permissão de assinatura** `com.implacavel.alarme.permission.IA`: só apps assinados com a mesma chave ligam no serviço.
- **Ação** `com.implacavel.alarme.action.IA`, pacote `com.implacavel.alarme`.

## Mensagens (`android.os.Message.what`)

| `what` | Sentido | Conteúdo |
|---|---|---|
| `1` PERGUNTAR | cliente → Tourbillon | `Bundle("instrucao": String, "prompt": String, "ate": Long opcional)` e `replyTo`. A instrução vira a do sistema; o prompt é a frase da pessoa |
| `2` RESPOSTA | Tourbillon → cliente | à PERGUNTAR: `Bundle("texto": String)`, vazio = não deu (o cliente usa as regras dele). À OCUPADO: `Bundle("ocupado": Boolean)` |
| `3` AQUECER | cliente → Tourbillon | `Bundle("instrucao": String)`: carrega o modelo e já lê a instrução; sem resposta |
| `4` OCUPADO | cliente → Tourbillon | "está com um alarme?" (o Chiron pergunta antes de tocar um lembrete); volta RESPOSTA com `"ocupado"` |

### A chave `"ate"` (o prazo)

`putLong("ate", System.currentTimeMillis() + espera)`: até quando o cliente espera. Se ela vier e já tiver passado, o
Tourbillon devolve RESPOSTA sem texto, **sem gerar**. É conferido na chegada e de novo no começo do trabalho, na thread
do modelo: uma pergunta abandonada não ocupa o modelo. Sem a chave, sem prazo (como antes; compatível).

## O contrato do motor (`ContratoDoMotor.kt`)

Os parâmetros de geração moram num lugar só e valem para todo cliente. O cliente manda só a instrução, a frase e o
prazo; cada app guarda a sua instrução e a leitura do JSON.

| O quê | Valor | Onde |
|---|---|---|
| Teto da resposta | 64 tokens (era 120) | `AVULSA_MAX_TOKENS` |
| Amostragem | `temperature` 0,2, `topK` 20, `topP` 0,9 | `AMOSTRAGEM_AVULSA` |
| Tempo máximo de geração | o teto: 64 tokens a 17–19 tokens/s na GPU, ~4 s no pior caso (era ~8 s) | `AVULSA_MAX_TOKENS` |
| Prazo | a chave `"ate"`; vencido = RESPOSTA vazia, sem gerar | `CHAVE_DO_PRAZO`, `semVez` |
| Cancelamento | nunca `cancelProcess()` (estraga a conversa para sempre, medido): quem desiste deixa o prazo vencer | `semVez` |
| Alarme na frente | alarme tocando ou em andamento, ou o teste da IA pelo cabo: RESPOSTA vazia na hora | `semVez`, `ServicoDeIa.alarmeNaFrente` |
| Modelo na CPU | a GPU não abriu (ou a CPU forçada pelo cabo): RESPOSTA vazia na hora (ler a instrução levaria 12–21 s) | `semVez` |
| Tamanho da entrada | instrução até 8000 letras (cortada com `Log.w`), frase até 1000 | `ServicoDeIa.INSTRUCAO_MAX`, `PERGUNTA_MAX` |
| Modelo parado | sai da memória 3 min depois da última pergunta, se não houver alarme | `ServicoDeIa.PARADO_MS` |

**Quando cada regra é conferida** (`semVez`, na ordem: alarme, prazo, CPU): na chegada (`ServicoDeIa.atender`), no
começo do trabalho na thread do modelo, de novo depois da primeira carga (é ela que diz se a GPU abriu) e antes da
"conversa reserva" (a próxima conversa limpa, já com a instrução lida).

**O que vale para todos, inclusive a conversa da manhã do Tourbillon** (é do motor, em `ModeloLocal`): a GPU primeiro
e a CPU se ela não abrir; uma thread só para tudo do modelo (as duas conversas nunca juntas: a da manhã fecha a do
cliente antes de abrir); contexto de 4096 tokens; sem "thinking"; nunca `cancelProcess()`. Todo ajuste genérico feito
por causa do Chiron entra aqui e vale para a manhã também. O que é de cada uso fica com ele: a manhã tem a amostragem
dela (`temperature` 0,9, para variar as falas), o teto dela (`RESPOSTA_MAX_TOKENS`, 72) e, na CPU, **continua
funcionando como antes** (só as perguntas de fora voltam vazias).

### A medida do teto (11-08)

Bateria 1b do Chiron (`bateria/saidas/rodada1b-*.jsonl`, campo `"ia"`, 339 respostas do modelo): a maior saída teve
111 letras (~32 tokens, contando ~3,5 letras por token), com as cercas de código. Desde o `88df146` do Chiron o
`"conversar"` traz `"fala"` de até 20 palavras: com 20 palavras de 6 letras e as cercas, ~186 letras, ~54 tokens. O teto
de 64 cabe as duas com folga (o teste `ContratoDoMotorTest` exige 10%). Passou do teto, o JSON sai cortado e o cliente
usa as regras dele.

## Testes pelo cabo (sem som)

- Conversa da manhã de mentira, na CPU ou na GPU: `adb shell am broadcast -n com.implacavel.alarme/.TesteDaIa [--ez gpu true]`
  e `adb logcat -s Implacavel`.
- O modelo compartilhado (o do Chiron e da manhã) só na CPU, sem rodar o teste:
  `adb shell am broadcast -n com.implacavel.alarme/.TesteDaIa --es modelo cpu`; de volta à GPU:
  `adb shell am broadcast -n com.implacavel.alarme/.TesteDaIa --es modelo gpu`. Vale até o "gpu" ou até o processo morrer.
  Não roda com alarme em andamento.
- O log das perguntas de fora: `adb logcat -s Implacavel | grep "IA do celular"` ("sem resposta (prazo vencido)",
  "(alarme na frente)", "(modelo na CPU)", "respondida em … ms", "conversa reserva … pronta").
