package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.dominio.ValorEntrada.Ausente;
import br.com.fiap.feedback.dominio.ValorEntrada.Inteiro;
import br.com.fiap.feedback.dominio.ValorEntrada.Outro;
import br.com.fiap.feedback.dominio.ValorEntrada.Texto;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorAvaliacaoTest {

    private static final ValorEntrada DESCRICAO_VALIDA = new Texto("Aula boa");
    private static final ValorEntrada NOTA_VALIDA = inteiro("5");
    private static final String EMOJI = "😀";

    private static ValorEntrada inteiro(String valor) {
        return new Inteiro(new BigInteger(valor));
    }

    private static List<String> validarDescricao(ValorEntrada descricao) {
        return ValidadorAvaliacao.validar(descricao, NOTA_VALIDA);
    }

    private static List<String> validarNota(ValorEntrada nota) {
        return ValidadorAvaliacao.validar(DESCRICAO_VALIDA, nota);
    }

    @Test
    void descricaoCom1CaractereAceita() {
        assertThat(validarDescricao(new Texto("a"))).isEmpty();
    }

    @Test
    void descricaoCom1000CaracteresAceita() {
        assertThat(validarDescricao(new Texto("a".repeat(1000)))).isEmpty();
    }

    @Test
    void descricaoCom1001CaracteresRejeitada() {
        assertThat(validarDescricao(new Texto("a".repeat(1001)))).containsExactly("descricao");
    }

    @Test
    void milEmojisContamComoMilCaracteres() {
        String descricao = EMOJI.repeat(1000);

        assertThat(descricao).hasSize(2000);
        assertThat(validarDescricao(new Texto(descricao))).isEmpty();
    }

    @Test
    void milEUmEmojisSaoRejeitados() {
        assertThat(validarDescricao(new Texto(EMOJI.repeat(1001)))).containsExactly("descricao");
    }

    @Test
    void espacosEntramNaContagemDoLimite() {
        assertThat(validarDescricao(new Texto("a" + " ".repeat(999)))).isEmpty();
        assertThat(validarDescricao(new Texto("a" + " ".repeat(1000)))).containsExactly("descricao");
    }

    @Test
    void descricaoAusenteOuNulaRejeitada() {
        assertThat(validarDescricao(new Ausente())).containsExactly("descricao");
    }

    @Test
    void descricaoQueNaoEhTextoRejeitada() {
        assertThat(validarDescricao(inteiro("123"))).containsExactly("descricao");
        assertThat(validarDescricao(new Outro())).containsExactly("descricao");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n", "\r\n", " \t\r\n "})
    void descricaoSemConteudoRejeitada(String descricao) {
        assertThat(validarDescricao(new Texto(descricao))).containsExactly("descricao");
    }

    // A spec só lista espaço, tabulação e quebra de linha, então estes caracteres contam como conteúdo.
    @ParameterizedTest
    @ValueSource(strings = {" ", " ", "　"})
    void outrosBrancosUnicodeContamComoConteudo(String descricao) {
        assertThat(validarDescricao(new Texto(descricao))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "5", "10"})
    void notaDentroDoIntervaloAceita(String nota) {
        assertThat(validarNota(inteiro(nota))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "11", "99999999999999999999", "-99999999999999999999"})
    void notaForaDoIntervaloRejeitada(String nota) {
        assertThat(validarNota(inteiro(nota))).containsExactly("nota");
    }

    @Test
    void notaAusenteOuNulaRejeitada() {
        assertThat(validarNota(new Ausente())).containsExactly("nota");
    }

    @Test
    void notaDecimalOuComExpoenteRejeitada() {
        assertThat(validarNota(new Outro())).containsExactly("nota");
    }

    @Test
    void notaComoTextoRejeitada() {
        assertThat(validarNota(new Texto("7"))).containsExactly("nota");
    }

    @Test
    void doisCamposInvalidosSaoCitados() {
        assertThat(ValidadorAvaliacao.validar(new Texto(""), inteiro("11")))
                .containsExactly("descricao", "nota");
    }

    @Test
    void entradaValidaNaoTemCampoInvalido() {
        assertThat(ValidadorAvaliacao.validar(DESCRICAO_VALIDA, NOTA_VALIDA)).isEmpty();
    }
}
