# GPS dentro de casa

**Resposta curta:** é limite físico. Não há ajuste de código que faça a caminhada contar dentro de casa sem abrir a porta pra trapaça.

## O que o código faz hoje
- Só o GPS puro: `LocationManager.GPS_PROVIDER`, 1 fixo por segundo, sem internet (`Caminhada.kt`, `SentidosDaCaminhada`). Liga no STOP, então já está quente na hora de sair.
- Aceita o fixo com precisão de até 20 m (`PRECISAO_MAX_M`; com 25 m, um fixo na cama perto da janela passava). Pega a velocidade pelo Doppler, de 0,6 a 4 m/s, e exige passos no ritmo e a posição andando junto com a velocidade em 12 s (`DetectorDeCaminhada`).
- Os passos sozinhos calam os bipes, mas não somam tempo, a não ser gastando a "confiança" juntada andando lá fora (até 90 s, pra um corredor coberto).

## Por que falha dentro de casa
- O sinal do satélite chega a uns -130 dBm. Uma laje de concreto tira mais 10 a 30 dB. No S24 FE os logs mostram 41 a 46 dB-Hz andando lá fora e 36 a 37 dB-Hz parado dentro de casa.
- Com esse sinal, o celular ainda dá uma posição, mas com erro acima de 20 m e sem velocidade Doppler confiável. A regra recusa, de propósito.
- O L1+L5 do S24 FE ajuda contra reflexo na rua, não contra parede.

## O que foi considerado e por que não entra
| Opção | Resultado |
|---|---|
| Provedor fundido (Play Services) | Dentro de casa ele cai pra Wi-Fi e rede: 15 a 40 m de erro, sem Doppler. Não confirma 5 m em 12 s. Lá fora, dá o mesmo que o GPS. Ainda traz uma dependência nova. |
| Wi-Fi ou rede | Precisão de cômodo, na melhor hipótese, e sem velocidade de caminhada. |
| Afrouxar a precisão ou a janela | O ruído de um celular parado passa a parecer caminhada. Desfaz o anti-trapaça (commits ac86b4f e c39180a). |
| Contador de passos sozinho | É o único que funciona dentro de casa, mas um celular sacudido engana ele: "sacode na cama e o alarme para". |
| Rastreio GNSS completo (`setFullTracking`) | Pode segurar satélite fraco sob árvore ou perto de prédio, gastando bateria. Não muda nada dentro de casa, e não tem como medir isso daqui. Não implementado. |

## O que usar no lugar
- A caminhada começa na porta de casa: o prazo pra sair (5 min) existe pra isso.
- Dia de chuva ou em que não dá pra sair: a missão da câmera, que já é oferecida ("☔ Switch to camera").
- O balanço forte que cala o bipe não depende de GPS: é só o acelerômetro, em qualquer modo.
