package br.com.fiap.feedback.registro;

import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Ponto de contato com o X-Ray. Existe para os testes não carregarem o recorder, que abre socket e
 * exige contexto de trace.
 */
interface Rastreador {

    /** Implementação de produção, com os utilitários do Powertools. */
    Rastreador XRAY = new Rastreador() {
        @Override
        public void emSubsegmento(String nome, Runnable acao) {
            TracingUtils.withSubsegment(nome, subsegmento -> acao.run());
        }

        @Override
        public void anotar(String chave, String valor) {
            TracingUtils.putAnnotation(chave, valor);
        }
    };

    void emSubsegmento(String nome, Runnable acao);

    void anotar(String chave, String valor);
}
