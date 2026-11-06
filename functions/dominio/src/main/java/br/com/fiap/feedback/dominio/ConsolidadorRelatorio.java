package br.com.fiap.feedback.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Consolida as avaliações lidas em um relatório, sem efeito externo.
 */
public final class ConsolidadorRelatorio {

    private static final int CASAS_DA_MEDIA = 2;

    private ConsolidadorRelatorio() {
    }

    /**
     * Monta o relatório de uma data de referência. Avaliação fora da janela não entra na lista, nos
     * totais nem na média, e a urgência é a persistida, sem recálculo a partir da nota.
     *
     * @param ambiente ambiente em execução
     * @param dataReferencia data de referência
     * @param avaliacoes avaliações lidas; a ordem dentro de cada dia é mantida
     * @return relatório com os 7 dias e as 3 urgências nos totais, mesmo com zero
     */
    public static RelatorioSemanal consolidar(
            String ambiente, DataReferencia dataReferencia, List<Avaliacao> avaliacoes) {
        JanelaApuracao janela = JanelaApuracao.de(dataReferencia);
        Map<String, List<Avaliacao>> porDia =
                avaliacoes.stream().collect(Collectors.groupingBy(Avaliacao::dataEnvio));

        Map<LocalDate, Integer> totaisPorDia = new TreeMap<>();
        List<Avaliacao> naJanela = new ArrayList<>();
        for (LocalDate dia : janela.dias()) {
            List<Avaliacao> doDia = porDia.getOrDefault(dia.toString(), List.of());
            totaisPorDia.put(dia, doDia.size());
            naJanela.addAll(doDia);
        }

        ResumoSemanal resumo = new ResumoSemanal(
                ambiente, dataReferencia, janela, media(naJanela), totaisPorUrgencia(naJanela), totaisPorDia);
        return new RelatorioSemanal(resumo, naJanela);
    }

    private static Map<Urgencia, Integer> totaisPorUrgencia(List<Avaliacao> avaliacoes) {
        Map<Urgencia, Integer> totais = new EnumMap<>(Urgencia.class);
        for (Urgencia urgencia : Urgencia.values()) {
            totais.put(urgencia, 0);
        }
        avaliacoes.forEach(avaliacao -> totais.merge(avaliacao.urgencia(), 1, Integer::sum));
        return totais;
    }

    // HALF_UP é o arredondamento que a pessoa espera ao conferir à mão: 5,125 vira 5,13.
    private static Optional<BigDecimal> media(List<Avaliacao> avaliacoes) {
        if (avaliacoes.isEmpty()) {
            return Optional.empty();
        }
        long soma = avaliacoes.stream().mapToLong(Avaliacao::nota).sum();
        return Optional.of(BigDecimal.valueOf(soma)
                .divide(BigDecimal.valueOf(avaliacoes.size()), CASAS_DA_MEDIA, RoundingMode.HALF_UP));
    }
}
