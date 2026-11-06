package br.com.fiap.feedback.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Números da semana, sem a lista de avaliações. É o que {@link AvisoRelatorio} recebe, então o aviso
 * não tem como incluir descrição nem nota individual.
 *
 * @param ambiente ambiente em execução
 * @param dataReferencia data de referência
 * @param janela janela apurada
 * @param media média das notas com duas casas, vazia quando a janela não tem avaliações
 * @param totaisPorUrgencia quantidade de cada uma das 3 urgências, na ordem do enum
 * @param totaisPorDia quantidade de cada um dos 7 dias, em ordem cronológica
 */
public record ResumoSemanal(
        String ambiente,
        DataReferencia dataReferencia,
        JanelaApuracao janela,
        Optional<BigDecimal> media,
        Map<Urgencia, Integer> totaisPorUrgencia,
        Map<LocalDate, Integer> totaisPorDia) {

    /** Texto apresentado no lugar da média quando a janela não tem avaliações. */
    public static final String SEM_MEDIA = "sem avaliações na janela";

    private static final int CASAS_DA_MEDIA = 2;

    /**
     * Valida os totais, que trazem sempre os 7 dias e as 3 urgências, mesmo com zero.
     *
     * @throws IllegalArgumentException se faltar dia ou urgência, ou se a média não tiver duas casas
     */
    public ResumoSemanal {
        Objects.requireNonNull(ambiente, "ambiente");
        Objects.requireNonNull(dataReferencia, "dataReferencia");
        Objects.requireNonNull(janela, "janela");
        Objects.requireNonNull(media, "media");
        if (media.filter(valor -> valor.scale() != CASAS_DA_MEDIA).isPresent()) {
            throw new IllegalArgumentException("A média precisa ter duas casas decimais.");
        }

        Map<Urgencia, Integer> urgencias = new EnumMap<>(Urgencia.class);
        urgencias.putAll(totaisPorUrgencia);
        if (urgencias.size() != Urgencia.values().length) {
            throw new IllegalArgumentException("Os totais precisam trazer as 3 urgências.");
        }
        totaisPorUrgencia = Collections.unmodifiableMap(urgencias);

        TreeMap<LocalDate, Integer> dias = new TreeMap<>(totaisPorDia);
        if (!new ArrayList<>(dias.keySet()).equals(janela.dias())) {
            throw new IllegalArgumentException("Os totais precisam trazer os 7 dias da janela.");
        }
        totaisPorDia = Collections.unmodifiableSortedMap(dias);
    }

    /**
     * Soma as avaliações da janela.
     *
     * @return quantidade total de avaliações
     */
    public int quantidadeTotal() {
        return totaisPorDia.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Apresenta a média sem depender do {@code Locale} do ambiente.
     *
     * @return média com vírgula e duas casas, como {@code 5,67}, ou {@link #SEM_MEDIA}
     */
    public String mediaFormatada() {
        return media.map(valor -> valor.toPlainString().replace('.', ',')).orElse(SEM_MEDIA);
    }

    // O toString gerado exporia a média a quem registrasse o objeto em log, e com uma única avaliação
    // na janela ela revela a nota.
    @Override
    public String toString() {
        return "ResumoSemanal[ambiente=" + ambiente
                + ", dataReferencia=" + dataReferencia
                + ", janela=" + janela
                + ", totaisPorUrgencia=" + totaisPorUrgencia
                + ", totaisPorDia=" + totaisPorDia + "]";
    }
}
