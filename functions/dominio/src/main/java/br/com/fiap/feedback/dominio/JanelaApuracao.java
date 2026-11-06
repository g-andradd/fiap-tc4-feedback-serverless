package br.com.fiap.feedback.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Os 7 dias civis anteriores à data de referência, com a própria referência fora.
 *
 * @param inicio primeiro dia, incluído
 * @param fim último dia, incluído
 */
public record JanelaApuracao(LocalDate inicio, LocalDate fim) {

    /** Quantidade de dias de toda janela. */
    public static final int QUANTIDADE_DIAS = 7;

    /**
     * Valida a duração da janela.
     *
     * @throws IllegalArgumentException se a janela não tiver exatamente 7 dias
     */
    public JanelaApuracao {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.equals(inicio.plusDays(QUANTIDADE_DIAS - 1L))) {
            throw new IllegalArgumentException("A janela precisa ter 7 dias.");
        }
    }

    /**
     * Calcula a janela de uma data de referência.
     *
     * @param referencia data de referência
     * @return janela correspondente
     */
    public static JanelaApuracao de(DataReferencia referencia) {
        LocalDate dia = referencia.valor();
        return new JanelaApuracao(dia.minusDays(QUANTIDADE_DIAS), dia.minusDays(1));
    }

    /**
     * Lista os dias da janela.
     *
     * @return os 7 dias, em ordem cronológica
     */
    public List<LocalDate> dias() {
        return inicio.datesUntil(fim.plusDays(1)).toList();
    }
}
