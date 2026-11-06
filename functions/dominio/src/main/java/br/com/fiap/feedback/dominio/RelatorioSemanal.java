package br.com.fiap.feedback.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Relatório completo: os números da semana e a lista de avaliações da janela.
 *
 * @param resumo números da semana
 * @param avaliacoes avaliações da janela, por dia e, no dia, na ordem de leitura
 */
public record RelatorioSemanal(ResumoSemanal resumo, List<Avaliacao> avaliacoes) {

    /**
     * Copia a lista, para o relatório continuar imutável.
     *
     * @throws NullPointerException se algum dos componentes for nulo
     */
    public RelatorioSemanal {
        Objects.requireNonNull(resumo, "resumo");
        avaliacoes = List.copyOf(avaliacoes);
    }
}
