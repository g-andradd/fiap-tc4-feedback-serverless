package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConsolidadorRelatorioTest {

    private static final DataReferencia REFERENCIA = new DataReferencia(LocalDate.of(2026, 11, 9));

    private static int sequencia;

    private static Avaliacao avaliacao(int nota, Urgencia urgencia, String dataEnvio) {
        sequencia++;
        return new Avaliacao(
                "id-" + sequencia, "Descrição " + sequencia, nota, urgencia, dataEnvio, dataEnvio + "T12:00:00.000Z");
    }

    private static Avaliacao avaliacao(int nota, String dataEnvio) {
        return avaliacao(nota, Urgencia.deNota(nota), dataEnvio);
    }

    private static Avaliacao enviadaEm(String instante) {
        return Avaliacao.nova("Aula", 5, "id-" + instante, Instant.parse(instante));
    }

    private static RelatorioSemanal consolidar(Avaliacao... avaliacoes) {
        return ConsolidadorRelatorio.consolidar("dev", REFERENCIA, List.of(avaliacoes));
    }

    private static String mediaDasNotas(int... notas) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        for (int nota : notas) {
            avaliacoes.add(avaliacao(nota, "2026-11-03"));
        }
        return ConsolidadorRelatorio.consolidar("dev", REFERENCIA, avaliacoes).resumo().mediaFormatada();
    }

    @Nested
    @DisplayName("Janela de apuração")
    class Janela {

        @Test
        @DisplayName("Avaliação no primeiro dia da janela entra")
        void primeiroDia() {
            assertThat(consolidar(avaliacao(5, "2026-11-02")).avaliacoes()).hasSize(1);
        }

        @Test
        @DisplayName("Avaliação no último dia da janela entra")
        void ultimoDia() {
            assertThat(consolidar(avaliacao(5, "2026-11-08")).avaliacoes()).hasSize(1);
        }

        @ParameterizedTest(name = "enviada em {0}")
        @ValueSource(strings = {"2026-11-09", "2026-11-01"})
        @DisplayName("Avaliação no dia da referência ou oito dias antes fica fora da lista, dos totais e da média")
        void foraDaJanela(String dataEnvio) {
            RelatorioSemanal relatorio = consolidar(avaliacao(2, dataEnvio), avaliacao(8, "2026-11-05"));

            assertThat(relatorio.avaliacoes()).hasSize(1);
            assertThat(relatorio.resumo().quantidadeTotal()).isEqualTo(1);
            assertThat(relatorio.resumo().totaisPorUrgencia())
                    .containsEntry(Urgencia.CRITICA, 0)
                    .containsEntry(Urgencia.BAIXA, 1);
            assertThat(relatorio.resumo().mediaFormatada()).isEqualTo("8,00");
        }

        @Test
        @DisplayName("Limite final avaliado no fuso de São Paulo")
        void limiteFinalEmSaoPaulo() {
            Avaliacao as2359 = enviadaEm("2026-11-09T02:59:00Z");
            Avaliacao as0000 = enviadaEm("2026-11-09T03:00:00Z");

            assertThat(consolidar(as2359, as0000).avaliacoes()).containsExactly(as2359);
        }

        @Test
        @DisplayName("Limite inicial avaliado no fuso de São Paulo")
        void limiteInicialEmSaoPaulo() {
            Avaliacao emUtcJaEhDia02 = enviadaEm("2026-11-02T02:59:00Z");

            assertThat(consolidar(emUtcJaEhDia02).avaliacoes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Conteúdo do relatório")
    class Conteudo {

        @Test
        @DisplayName("Semana com avaliações")
        void semanaComAvaliacoes() {
            Avaliacao critica = avaliacao(2, "2026-11-03");
            Avaliacao media = avaliacao(5, "2026-11-03");
            Avaliacao baixa = avaliacao(8, "2026-11-05");

            RelatorioSemanal relatorio = consolidar(critica, media, baixa);

            assertThat(relatorio.avaliacoes()).containsExactly(critica, media, baixa);
            assertThat(relatorio.resumo().totaisPorDia())
                    .containsEntry(LocalDate.of(2026, 11, 3), 2)
                    .containsEntry(LocalDate.of(2026, 11, 5), 1);
            assertThat(relatorio.resumo().totaisPorUrgencia()).containsExactlyEntriesOf(totais(1, 1, 1));
            assertThat(relatorio.resumo().mediaFormatada()).isEqualTo("5,00");
            assertThat(relatorio.resumo().quantidadeTotal()).isEqualTo(3);
        }

        @Test
        @DisplayName("A lista sai por dia e mantém a ordem de leitura dentro do dia")
        void ordem() {
            Avaliacao diaCinco = avaliacao(5, "2026-11-05");
            Avaliacao diaTresPrimeira = avaliacao(5, "2026-11-03");
            Avaliacao diaTresSegunda = avaliacao(5, "2026-11-03");

            assertThat(consolidar(diaCinco, diaTresPrimeira, diaTresSegunda).avaliacoes())
                    .containsExactly(diaTresPrimeira, diaTresSegunda, diaCinco);
        }

        @Test
        @DisplayName("Urgência vem do que foi persistido, sem recálculo a partir da nota")
        void urgenciaPersistida() {
            Avaliacao notaAltaComoCritica = avaliacao(9, Urgencia.CRITICA, "2026-11-03");

            RelatorioSemanal relatorio = consolidar(notaAltaComoCritica);

            assertThat(relatorio.avaliacoes().getFirst().urgencia()).isEqualTo(Urgencia.CRITICA);
            assertThat(relatorio.resumo().totaisPorUrgencia())
                    .containsEntry(Urgencia.CRITICA, 1)
                    .containsEntry(Urgencia.BAIXA, 0);
        }
    }

    @Nested
    @DisplayName("Totais completos")
    class TotaisCompletos {

        @Test
        @DisplayName("Dias e urgências sem avaliações aparecem com zero, em ordem")
        void diasEUrgenciasSemAvaliacoes() {
            RelatorioSemanal relatorio = consolidar(avaliacao(8, "2026-11-03"));

            assertThat(relatorio.resumo().totaisPorDia()).containsExactly(
                    Map.entry(LocalDate.of(2026, 11, 2), 0),
                    Map.entry(LocalDate.of(2026, 11, 3), 1),
                    Map.entry(LocalDate.of(2026, 11, 4), 0),
                    Map.entry(LocalDate.of(2026, 11, 5), 0),
                    Map.entry(LocalDate.of(2026, 11, 6), 0),
                    Map.entry(LocalDate.of(2026, 11, 7), 0),
                    Map.entry(LocalDate.of(2026, 11, 8), 0));
            assertThat(relatorio.resumo().totaisPorUrgencia()).containsExactlyEntriesOf(totais(0, 0, 1));
        }

        @Test
        @DisplayName("Janela vazia traz os 7 dias e as 3 urgências em zero, sem média")
        void janelaVazia() {
            RelatorioSemanal relatorio = consolidar();

            assertThat(relatorio.avaliacoes()).isEmpty();
            assertThat(relatorio.resumo().totaisPorDia()).hasSize(7).allSatisfy((dia, total) -> assertThat(total).isZero());
            assertThat(relatorio.resumo().totaisPorUrgencia()).containsExactlyEntriesOf(totais(0, 0, 0));
            assertThat(relatorio.resumo().media()).isEmpty();
            assertThat(relatorio.resumo().quantidadeTotal()).isZero();
        }
    }

    private static Map<Urgencia, Integer> totais(int critica, int media, int baixa) {
        Map<Urgencia, Integer> totais = new EnumMap<>(Urgencia.class);
        totais.put(Urgencia.CRITICA, critica);
        totais.put(Urgencia.MEDIA, media);
        totais.put(Urgencia.BAIXA, baixa);
        return totais;
    }

    @Nested
    @DisplayName("Média das notas")
    class Media {

        @Test
        @DisplayName("Notas 5, 6 e 6 dão 5,67")
        void naoExata() {
            assertThat(mediaDasNotas(5, 6, 6)).isEqualTo("5,67");
        }

        @Test
        @DisplayName("Notas 2, 5 e 8 dão 5,00")
        void exata() {
            assertThat(mediaDasNotas(2, 5, 8)).isEqualTo("5,00");
        }

        @Test
        @DisplayName("Só notas 0 dão 0,00, que não se confunde com ausência de média")
        void soZeros() {
            assertThat(mediaDasNotas(0, 0)).isEqualTo("0,00");
        }

        @Test
        @DisplayName("Soma 41 em 8 avaliações dá 5,13: o empate sobe")
        void empateSobe() {
            assertThat(mediaDasNotas(5, 5, 5, 5, 5, 5, 5, 6)).isEqualTo("5,13");
        }

        @Test
        @DisplayName("Janela vazia não tem valor de média")
        void janelaVazia() {
            ResumoSemanal resumo = consolidar().resumo();

            assertThat(resumo.media()).isEmpty();
            assertThat(resumo.mediaFormatada()).isEqualTo("sem avaliações na janela");
        }

        @Test
        @DisplayName("Nota máxima em todas as avaliações")
        void notaMaxima() {
            assertThat(mediaDasNotas(10, 10, 10)).isEqualTo("10,00");
        }

        @ParameterizedTest(name = "Locale {0}")
        @ValueSource(strings = {"pt-BR", "en-US", "de-DE", "ar-SA", "hi-IN"})
        @DisplayName("A saída não depende do Locale do ambiente")
        void semDependerDoLocale(String idioma) {
            Locale original = Locale.getDefault();
            Locale.setDefault(Locale.forLanguageTag(idioma));
            try {
                assertThat(mediaDasNotas(5, 6, 6)).isEqualTo("5,67");
            } finally {
                Locale.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("Invariantes do resumo")
    class Invariantes {

        private final JanelaApuracao janela = JanelaApuracao.de(REFERENCIA);
        private final Map<LocalDate, Integer> dias = consolidar().resumo().totaisPorDia();
        private final Map<Urgencia, Integer> urgencias = totais(0, 0, 0);

        @Test
        @DisplayName("Falta de urgência é rejeitada")
        void faltaUrgencia() {
            assertThatThrownBy(() -> new ResumoSemanal(
                    "dev", REFERENCIA, janela, Optional.empty(), Map.of(Urgencia.BAIXA, 1), dias))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Falta de dia é rejeitada")
        void faltaDia() {
            assertThatThrownBy(() -> new ResumoSemanal(
                    "dev", REFERENCIA, janela, Optional.empty(), urgencias, Map.of(LocalDate.of(2026, 11, 2), 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Média sem duas casas é rejeitada")
        void mediaSemDuasCasas() {
            assertThatThrownBy(() -> new ResumoSemanal(
                    "dev", REFERENCIA, janela, Optional.of(BigDecimal.valueOf(5)), urgencias, dias))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Os totais e a lista não podem ser alterados depois de prontos")
        void imutavel() {
            RelatorioSemanal relatorio = consolidar(avaliacao(5, "2026-11-03"));

            assertThatThrownBy(() -> relatorio.resumo().totaisPorDia().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> relatorio.resumo().totaisPorUrgencia().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> relatorio.avaliacoes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("O texto do objeto não expõe descrição, nota nem média")
        void toStringSemDadoDoAluno() {
            RelatorioSemanal relatorio = ConsolidadorRelatorio.consolidar(
                    "dev",
                    REFERENCIA,
                    List.of(new Avaliacao("id-1", "TEXTO-MARCADOR", 7, Urgencia.BAIXA, "2026-11-03", "x")));

            assertThat(relatorio.toString()).doesNotContain("TEXTO-MARCADOR", "7,00", "7.00", "nota");
        }
    }
}
