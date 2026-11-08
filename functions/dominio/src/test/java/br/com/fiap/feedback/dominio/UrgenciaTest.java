package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class UrgenciaTest {

    @ParameterizedTest(name = "nota {0} vira {1}")
    @CsvSource({
        "0, CRITICA",
        "3, CRITICA",
        "4, MEDIA",
        "6, MEDIA",
        "7, BAIXA",
        "10, BAIXA"
    })
    void classificaOsLimitesDaTabela(int nota, Urgencia esperada) {
        assertThat(Urgencia.deNota(nota)).isEqualTo(esperada);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void rejeitaNotaForaDoIntervaloSemCitarOValor(int nota) {
        assertThatThrownBy(() -> Urgencia.deNota(nota))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(String.valueOf(nota));
    }
}
