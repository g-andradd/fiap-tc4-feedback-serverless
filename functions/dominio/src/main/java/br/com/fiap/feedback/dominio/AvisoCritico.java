package br.com.fiap.feedback.dominio;

import java.util.Objects;

/**
 * Aviso enviado aos administradores quando uma avaliação crítica é registrada.
 *
 * <p>O texto usa só valores persistidos na avaliação. Por isso sai igual em qualquer reprocessamento
 * e traz a data de envio do item, nunca a data em que o aviso foi publicado.
 *
 * @param assunto assunto da mensagem
 * @param corpo texto puro com urgência, data de envio e descrição completa
 */
public record AvisoCritico(String assunto, String corpo) {

    private static final String ASSUNTO = "Avaliação crítica registrada";

    /**
     * Valida os atributos. As mensagens citam só o nome do campo, nunca o valor.
     *
     * @throws IllegalArgumentException se o assunto ou o corpo estiverem nulos ou vazios
     */
    public AvisoCritico {
        exigirTexto(assunto, "assunto");
        exigirTexto(corpo, "corpo");
    }

    /**
     * Monta o aviso de uma avaliação crítica, com a descrição sem corte nem alteração.
     *
     * @param avaliacao avaliação lida do item persistido
     * @return aviso pronto para publicar
     * @throws IllegalArgumentException se a urgência da avaliação não for {@link Urgencia#CRITICA}
     */
    public static AvisoCritico de(Avaliacao avaliacao) {
        Objects.requireNonNull(avaliacao, "avaliacao");
        if (avaliacao.urgencia() != Urgencia.CRITICA) {
            throw new IllegalArgumentException("Só avaliação crítica gera aviso.");
        }
        String corpo = "Uma avaliação crítica foi registrada.\n"
                + "\n"
                + "Urgência: " + avaliacao.urgencia().name() + "\n"
                + "Data de envio: " + avaliacao.dataEnvio() + "\n"
                + "\n"
                + "Descrição:\n"
                + avaliacao.descricao();
        return new AvisoCritico(ASSUNTO, corpo);
    }

    // O toString gerado pelo record exporia a descrição a quem registrasse o objeto em log.
    @Override
    public String toString() {
        return "AvisoCritico[assunto=" + assunto + "]";
    }

    private static void exigirTexto(String valor, String nome) {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("Campo inválido: " + nome);
        }
    }
}
