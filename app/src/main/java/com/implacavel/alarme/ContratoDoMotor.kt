// O contrato do motor compartilhado: o que vale pra todo cliente que pergunta de fora ([ServicoDeIa]; hoje, o Chiron),
// num lugar só. (o dono, 11-08): "os parâmetros de geração ficam num lugar só do Tourbillon e valem para todos os
// clientes; cada app guarda só a sua instrução e a leitura do JSON". O cliente manda a instrução, a frase e, se quiser,
// o prazo; o resto é daqui. O que é do motor ([ModeloLocal]: a GPU primeiro, uma thread só, uma conversa limpa por
// pedido, nunca cancelProcess) vale também pra conversa da manhã: um ajuste genérico feito por causa do Chiron entra lá.
// O protocolo inteiro (mensagens, chaves, tempos) está em docs/IA-compartilhada.md.
package com.implacavel.alarme

import com.google.ai.edge.litertlm.SamplerConfig

/** As perguntas de fora (escolher uma habilidade e responder em JSON): temperatura bem baixa, sem invenção. */
internal val AMOSTRAGEM_AVULSA = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.2)

/**
 * Tamanho máximo da resposta de uma pergunta de fora (uma linha JSON). (o dono, 11-08): era 120; medido na bateria 1b
 * (339 respostas), a maior saída teve 111 letras (~32 tokens); o "conversar" com a fala de até 20 palavras dá ~54. Com
 * 17 a 19 tokens por segundo na GPU, o pior caso cai de ~8 s pra ~4 s. Passou do teto, o JSON sai cortado e valem as
 * regras do cliente.
 */
const val AVULSA_MAX_TOKENS = 64
