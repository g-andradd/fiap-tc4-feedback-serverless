package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CarimboDeTempoTest {

    @Test
    void ultimoSegundoDoDiaEmSaoPauloFicaNoDiaAnterior() {
        Instant instante = Instant.parse("2026-11-10T02:59:59Z");

        assertThat(CarimboDeTempo.dataEnvio(instante)).isEqualTo("2026-11-09");
        assertThat(CarimboDeTempo.criadoEm(instante)).isEqualTo("2026-11-10T02:59:59.000Z");
        assertThat(Instant.parse(CarimboDeTempo.criadoEm(instante))).isEqualTo(instante);
    }

    @Test
    void primeiroSegundoDoDiaEmSaoPauloJaEhONovoDia() {
        Instant instante = Instant.parse("2026-11-10T03:00:00Z");

        assertThat(CarimboDeTempo.dataEnvio(instante)).isEqualTo("2026-11-10");
        assertThat(CarimboDeTempo.criadoEm(instante)).isEqualTo("2026-11-10T03:00:00.000Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2026-11-10T02:59:59Z",
        "2026-11-10T02:59:59.1Z",
        "2026-11-10T02:59:59.120Z",
        "2026-11-10T02:59:59.123456789Z",
        "2026-01-01T00:00:00Z"
    })
    void criadoEmTemSempre24Caracteres(String instante) {
        assertThat(CarimboDeTempo.criadoEm(Instant.parse(instante))).hasSize(24).endsWith("Z");
    }

    @Test
    void truncaAFracaoEmMilissegundosSemArredondar() {
        assertThat(CarimboDeTempo.criadoEm(Instant.parse("2026-11-10T02:59:59.999999999Z")))
                .isEqualTo("2026-11-10T02:59:59.999Z");
    }

    @Test
    void ordemLexicograficaEhIgualACronologica() {
        List<Instant> cronologica = List.of(
                Instant.parse("2026-11-09T23:59:59.999Z"),
                Instant.parse("2026-11-10T02:59:59Z"),
                Instant.parse("2026-11-10T02:59:59.001Z"),
                Instant.parse("2026-11-10T02:59:59.123Z"),
                Instant.parse("2026-11-10T02:59:59.900Z"),
                Instant.parse("2026-11-10T03:00:00Z"));

        List<String> formatados = cronologica.stream().map(CarimboDeTempo::criadoEm).toList();

        assertThat(formatados).isSortedAccordingTo(Comparator.naturalOrder());
    }
}
