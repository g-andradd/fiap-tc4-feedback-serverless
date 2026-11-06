package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatadorRelatorioTest {

    private static final DataReferencia REFERENCIA = new DataReferencia(LocalDate.of(2026, 11, 9));

    private static Avaliacao avaliacao(String id, String descricao, int nota, String dataEnvio) {
        return new Avaliacao(id, descricao, nota, Urgencia.deNota(nota), dataEnvio, dataEnvio + "T12:00:00.000Z");
    }

    private static String formatar(Avaliacao... avaliacoes) {
        return FormatadorRelatorio.formatar(
                ConsolidadorRelatorio.consolidar("dev", REFERENCIA, List.of(avaliacoes)));
    }

    @Test
    @DisplayName("Texto completo de uma semana com avaliações")
    void textoCompleto() {
        String texto = formatar(
                avaliacao("a", "Aula confusa", 2, "2026-11-03"),
                avaliacao("b", "Aula regular", 6, "2026-11-03"),
                avaliacao("c", "Aula ótima", 9, "2026-11-05"));

        assertThat(texto).isEqualTo("""
                RELATÓRIO SEMANAL DE AVALIAÇÕES
                Ambiente: dev
                Data de referência: 2026-11-09
                Janela: 2026-11-02 a 2026-11-08

                Média das notas: 5,67

                Quantidade por urgência
                CRITICA: 1
                MEDIA: 1
                BAIXA: 1

                Quantidade por dia
                2026-11-02: 0
                2026-11-03: 2
                2026-11-04: 0
                2026-11-05: 1
                2026-11-06: 0
                2026-11-07: 0
                2026-11-08: 0

                Avaliações (3)

                Data de envio: 2026-11-03 | Urgência: CRITICA
                Descrição: Aula confusa

                Data de envio: 2026-11-03 | Urgência: MEDIA
                Descrição: Aula regular

                Data de envio: 2026-11-05 | Urgência: BAIXA
                Descrição: Aula ótima
                """);
    }

    @Test
    @DisplayName("Semana vazia: lista vazia, zeros e o texto no lugar da média")
    void semanaVazia() {
        assertThat(formatar()).isEqualTo("""
                RELATÓRIO SEMANAL DE AVALIAÇÕES
                Ambiente: dev
                Data de referência: 2026-11-09
                Janela: 2026-11-02 a 2026-11-08

                Média das notas: sem avaliações na janela

                Quantidade por urgência
                CRITICA: 0
                MEDIA: 0
                BAIXA: 0

                Quantidade por dia
                2026-11-02: 0
                2026-11-03: 0
                2026-11-04: 0
                2026-11-05: 0
                2026-11-06: 0
                2026-11-07: 0
                2026-11-08: 0

                Avaliações (0)
                """);
    }

    @Test
    @DisplayName("Média zero aparece como 0,00")
    void mediaZero() {
        assertThat(formatar(avaliacao("a", "Aula péssima", 0, "2026-11-03")))
                .contains("Média das notas: 0,00\n")
                .doesNotContain("sem avaliações na janela");
    }

    @Test
    @DisplayName("A nota individual não aparece no relatório")
    void semNotaIndividual() {
        // Nota 9 sozinha na janela: a média seria 9,00, então um 9 solto só viria da nota.
        String texto = formatar(avaliacao("a", "Aula ótima", 9, "2026-11-03"));

        assertThat(texto.replace("9,00", "").replace("2026-11-09", "").replace("Aula ótima", ""))
                .doesNotContain("9")
                .doesNotContain("Nota")
                .doesNotContain("nota:");
    }

    @Test
    @DisplayName("Descrição aparece como persistida, inclusive com espaços e quebra de linha")
    void descricaoComoPersistida() {
        assertThat(formatar(avaliacao("a", "  linha 1\nlinha 2 | fim  ", 5, "2026-11-03")))
                .contains("Descrição:   linha 1\nlinha 2 | fim  \n");
    }
}
