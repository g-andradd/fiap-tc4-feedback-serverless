package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DataReferenciaTest {

    // 22:00 de 03/11/2026 em São Paulo, quando em UTC já é 04/11.
    private static final Clock NOITE_DE_03_11 =
            Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Valor ausente ou nulo usa o dia atual em São Paulo, não o dia em UTC")
    void ausenteUsaDiaAtualEmSaoPaulo() {
        assertThat(DataReferencia.de(null, NOITE_DE_03_11).valor()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    @DisplayName("Relógio em outro fuso não muda o dia calculado")
    void fusoDoRelogioNaoImporta() {
        Clock relogio = NOITE_DE_03_11.withZone(ZoneId.of("Asia/Tokyo"));

        assertThat(DataReferencia.de(null, relogio).valor()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    @DisplayName("Primeiro segundo do dia em São Paulo")
    void viradaDoDia() {
        Clock relogio = Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), ZoneOffset.UTC);

        assertThat(DataReferencia.de(null, relogio).valor()).isEqualTo(LocalDate.of(2026, 11, 4));
    }

    @Test
    @DisplayName("Data válida é usada como informada, inclusive no futuro")
    void dataValida() {
        assertThat(DataReferencia.de("2026-11-04", NOITE_DE_03_11).valor()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(DataReferencia.de("2030-01-01", NOITE_DE_03_11).valor()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    @DisplayName("Data no passado é aceita")
    void dataNoPassado() {
        assertThat(DataReferencia.de("2026-10-26", NOITE_DE_03_11).valor()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    @DisplayName("Dia 29 de fevereiro só existe em ano bissexto")
    void anoBissexto() {
        assertThat(DataReferencia.de("2028-02-29", NOITE_DE_03_11).valor()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThatThrownBy(() -> DataReferencia.de("2026-02-29", NOITE_DE_03_11))
                .isInstanceOf(DataReferenciaInvalidaException.class);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
        "09/11/2026",
        "2026-02-30",
        "2026-13-01",
        "",
        " ",
        "+12026-01-01",
        "-2026-01-01",
        "2026-1-1",
        "2026-11-04 ",
        " 2026-11-04",
        "2026-11-04\n",
        "2026-11-04T00:00:00",
        "٢٠٢٦-١١-٠٤",
        "hoje"
    })
    @DisplayName("Texto em outro formato, vazio ou com data inexistente é rejeitado")
    void textoInvalido(String valor) {
        assertThatThrownBy(() -> DataReferencia.de(valor, NOITE_DE_03_11))
                .isInstanceOf(DataReferenciaInvalidaException.class);
    }

    private static Stream<Object> valoresQueNaoSaoTexto() {
        return Stream.of(20261104, 2026.11, true, List.of("2026-11-04"), LocalDate.of(2026, 11, 4));
    }

    @ParameterizedTest
    @MethodSource("valoresQueNaoSaoTexto")
    @DisplayName("Valor que não é texto é rejeitado")
    void naoTexto(Object valor) {
        assertThatThrownBy(() -> DataReferencia.de(valor, NOITE_DE_03_11))
                .isInstanceOf(DataReferenciaInvalidaException.class);
    }

    @Test
    @DisplayName("A mensagem diz que a data é inválida, sem citar o valor nem encadear a causa")
    void mensagemSemOValor() {
        assertThatThrownBy(() -> DataReferencia.de("2026-02-30", NOITE_DE_03_11))
                .hasMessageContaining("Data de referência inválida")
                .hasMessageNotContaining("2026-02-30")
                .hasNoCause();
    }

    @Test
    @DisplayName("Dia nulo é rejeitado no construtor")
    void diaNulo() {
        assertThatThrownBy(() -> new DataReferencia(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("O texto da data segue o formato yyyy-MM-dd")
    void texto() {
        assertThat(new DataReferencia(LocalDate.of(2026, 1, 5))).hasToString("2026-01-05");
    }
}
