package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JanelaApuracaoTest {

    @ParameterizedTest(name = "referência {0}: de {1} a {2}")
    @CsvSource({
        "2026-11-09, 2026-11-02, 2026-11-08",
        "2026-11-03, 2026-10-27, 2026-11-02",
        "2026-11-04, 2026-10-28, 2026-11-03",
        "2026-10-26, 2026-10-19, 2026-10-25",
        "2027-01-03, 2026-12-27, 2027-01-02"
    })
    @DisplayName("A janela são os 7 dias anteriores à referência, com ela fora")
    void limitesDaJanela(LocalDate referencia, LocalDate inicio, LocalDate fim) {
        JanelaApuracao janela = JanelaApuracao.de(new DataReferencia(referencia));

        assertThat(janela.inicio()).isEqualTo(inicio);
        assertThat(janela.fim()).isEqualTo(fim);
    }

    @Test
    @DisplayName("Numa segunda, a janela vai de segunda a domingo da semana anterior")
    void janelaDeUmaSegunda() {
        JanelaApuracao janela = JanelaApuracao.de(new DataReferencia(LocalDate.of(2026, 11, 9)));

        assertThat(janela.inicio().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(janela.fim().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    @DisplayName("Os dias vêm em ordem cronológica, com os dois extremos incluídos")
    void dias() {
        JanelaApuracao janela = JanelaApuracao.de(new DataReferencia(LocalDate.of(2026, 11, 9)));

        assertThat(janela.dias()).containsExactly(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 6),
                LocalDate.of(2026, 11, 7),
                LocalDate.of(2026, 11, 8));
    }

    @Test
    @DisplayName("Janela com duração diferente de 7 dias é rejeitada")
    void duracaoErrada() {
        assertThatThrownBy(() -> new JanelaApuracao(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
