package br.com.fiap.feedback.dominio;

/**
 * Data de referência rejeitada antes de qualquer leitura. A mensagem é fixa e não cita o valor
 * recebido, porque o runtime do Lambda a escreve no log e na resposta da invocação.
 */
public final class DataReferenciaInvalidaException extends RuntimeException {

    /** Cria a exceção com a mensagem fixa. */
    public DataReferenciaInvalidaException() {
        super("Data de referência inválida. Informe uma data existente no formato yyyy-MM-dd.");
    }
}
