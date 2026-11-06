package br.com.fiap.feedback.dominio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Data que define a janela do relatório e identifica o objeto gravado.
 *
 * @param valor dia de referência
 */
public record DataReferencia(LocalDate valor) {

    // LocalDate.parse sozinho aceitaria ano com sinal, como +12026-01-01.
    private static final Pattern FORMATO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /**
     * Exige o dia de referência.
     *
     * @throws NullPointerException se o dia for nulo
     */
    public DataReferencia {
        Objects.requireNonNull(valor, "valor");
    }

    /**
     * Interpreta o valor recebido no evento.
     *
     * @param informada valor do campo {@code dataReferencia}, ausente quando {@code null}
     * @param relogio relógio usado quando nenhuma data é informada
     * @return a data informada ou, sem data, o dia atual em America/Sao_Paulo
     * @throws DataReferenciaInvalidaException se o valor não for um texto no formato
     *     {@code yyyy-MM-dd} com uma data que existe no calendário
     */
    public static DataReferencia de(Object informada, Clock relogio) {
        if (informada == null) {
            return new DataReferencia(LocalDate.now(relogio.withZone(CarimboDeTempo.ZONA_SAO_PAULO)));
        }
        if (!(informada instanceof String texto) || !FORMATO.matcher(texto).matches()) {
            throw new DataReferenciaInvalidaException();
        }
        try {
            return new DataReferencia(LocalDate.parse(texto));
        } catch (DateTimeParseException e) {
            throw new DataReferenciaInvalidaException();
        }
    }

    /**
     * Devolve a data em texto.
     *
     * @return dia no formato {@code yyyy-MM-dd}
     */
    @Override
    public String toString() {
        return valor.toString();
    }
}
