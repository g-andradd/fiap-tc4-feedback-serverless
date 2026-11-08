package br.com.fiap.feedback.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.dominio.ValorEntrada;
import br.com.fiap.feedback.dominio.ValorEntrada.Ausente;
import br.com.fiap.feedback.dominio.ValorEntrada.Inteiro;
import br.com.fiap.feedback.dominio.ValorEntrada.Outro;
import br.com.fiap.feedback.dominio.ValorEntrada.Texto;
import br.com.fiap.feedback.registro.LeitorCorpo.EntradaAvaliacao;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LeitorCorpoTest {

    private static EntradaAvaliacao ler(String corpo) throws CorpoInvalidoException {
        return LeitorCorpo.ler(corpo, false);
    }

    private static ValorEntrada lerNota(String nota) throws CorpoInvalidoException {
        return ler("{\"descricao\":\"Aula boa\",\"nota\":" + nota + "}").nota();
    }

    private static String emBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void leTextoEInteiro() throws Exception {
        EntradaAvaliacao entrada = ler("{\"descricao\":\"  Aula boa  \",\"nota\":7}");

        assertThat(entrada.descricao()).isEqualTo(new Texto("  Aula boa  "));
        assertThat(entrada.nota()).isEqualTo(new Inteiro(BigInteger.valueOf(7)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.0", "7.5", "1e1", "1E1", "1e400", "true", "[7]", "{\"valor\":7}"})
    void numeroNaoInteiroEOutrosTiposViramOutro(String nota) throws Exception {
        assertThat(lerNota(nota)).isEqualTo(new Outro());
    }

    @Test
    void notaComoTextoViraTexto() throws Exception {
        assertThat(lerNota("\"7\"")).isEqualTo(new Texto("7"));
    }

    @Test
    void nuloViraAusente() throws Exception {
        assertThat(lerNota("null")).isEqualTo(new Ausente());
        assertThat(ler("{\"descricao\":null,\"nota\":1}").descricao()).isEqualTo(new Ausente());
    }

    @Test
    void campoAusenteViraAusente() throws Exception {
        EntradaAvaliacao entrada = ler("{}");

        assertThat(entrada.descricao()).isEqualTo(new Ausente());
        assertThat(entrada.nota()).isEqualTo(new Ausente());
    }

    @Test
    void inteiroEnormeNaoEstoura() throws Exception {
        assertThat(lerNota("99999999999999999999"))
                .isEqualTo(new Inteiro(new BigInteger("99999999999999999999")));
    }

    @Test
    void numeroAcimaDoLimiteDoJacksonEhCorpoInvalido() {
        assertThatThrownBy(() -> lerNota("1".repeat(1001))).isInstanceOf(CorpoInvalidoException.class);
    }

    @Test
    void aninhamentoExcessivoEhCorpoInvalido() {
        String corpo = "{\"nota\":" + "[".repeat(100_000);

        assertThatThrownBy(() -> ler(corpo)).isInstanceOf(CorpoInvalidoException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void corpoNuloOuVazioEhInvalido(String corpo) {
        assertThatThrownBy(() -> ler(corpo)).isInstanceOf(CorpoInvalidoException.class);
        assertThatThrownBy(() -> LeitorCorpo.ler(corpo, true)).isInstanceOf(CorpoInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "   ",
        "{\"descricao\": \"Aula boa\", \"nota\": 5",
        "[1, 2, 3]",
        "\"texto\"",
        "7",
        "null",
        "{\"descricao\":\"Aula boa\",\"nota\":5} xx",
        "{\"descricao\":\"Aula boa\",\"nota\":5} {}",
        "{\"nota\":07}",
        "{'nota':7}"
    })
    void corpoQueNaoEhObjetoJsonBemFormadoEhInvalido(String corpo) {
        assertThatThrownBy(() -> ler(corpo)).isInstanceOf(CorpoInvalidoException.class);
    }

    @Test
    void decodificaBase64() throws Exception {
        String json = "{\"descricao\":\"Avaliação ótima\",\"nota\":3}";

        EntradaAvaliacao entrada = LeitorCorpo.ler(emBase64(json.getBytes(StandardCharsets.UTF_8)), true);

        assertThat(entrada.descricao()).isEqualTo(new Texto("Avaliação ótima"));
        assertThat(entrada.nota()).isEqualTo(new Inteiro(BigInteger.valueOf(3)));
    }

    @Test
    void base64InvalidoEhCorpoInvalido() {
        assertThatThrownBy(() -> LeitorCorpo.ler("isto não é base64!", true))
                .isInstanceOf(CorpoInvalidoException.class);
    }

    @Test
    void base64ComJsonMalformadoEhCorpoInvalido() {
        String corpo = emBase64("[1, 2, 3]".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> LeitorCorpo.ler(corpo, true)).isInstanceOf(CorpoInvalidoException.class);
    }

    @Test
    void base64ComUtf8InvalidoEhCorpoInvalido() {
        byte[] bytes = {'{', '"', 'a', '"', ':', '"', (byte) 0xC3, (byte) 0x28, '"', '}'};

        assertThatThrownBy(() -> LeitorCorpo.ler(emBase64(bytes), true))
                .isInstanceOf(CorpoInvalidoException.class);
    }

    @Test
    void excecaoNaoCarregaTrechoDoCorpoNemCausaNemPilha() {
        String corpo = "{\"descricao\": \"marcador-unico\", \"nota\": 5";

        assertThatThrownBy(() -> ler(corpo))
                .isInstanceOfSatisfying(CorpoInvalidoException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain("marcador-unico");
                    assertThat(e).hasNoCause();
                    assertThat(e.getStackTrace()).isEmpty();
                });
    }
}
