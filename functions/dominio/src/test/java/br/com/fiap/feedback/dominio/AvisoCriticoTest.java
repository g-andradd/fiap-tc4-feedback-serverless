package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AvisoCriticoTest {

    private static final String DATA_ENVIO = "2026-11-03";
    private static final String CRIADO_EM = "2026-11-03T15:04:05.000Z";

    private static Avaliacao critica(String descricao) {
        return new Avaliacao("id-1", descricao, 2, Urgencia.CRITICA, DATA_ENVIO, CRIADO_EM);
    }

    @Test
    @DisplayName("Assunto e corpo seguem o formato do design")
    void formatoCompleto() {
        AvisoCritico aviso = AvisoCritico.de(critica("Aula ruim"));

        assertThat(aviso.assunto()).isEqualTo("Avaliação crítica registrada");
        assertThat(aviso.corpo()).isEqualTo("""
                Uma avaliação crítica foi registrada.

                Urgência: CRITICA
                Data de envio: 2026-11-03

                Descrição:
                Aula ruim""");
    }

    @Test
    @DisplayName("Campos obrigatórios presentes")
    void camposObrigatorios() {
        String corpo = AvisoCritico.de(critica("Professor não apareceu")).corpo();

        assertThat(corpo).contains("Professor não apareceu", "CRITICA", DATA_ENVIO);
    }

    @Test
    @DisplayName("O assunto cabe no limite do SNS, sem quebra de linha")
    void assuntoDentroDoLimite() {
        String assunto = AvisoCritico.de(critica("Aula ruim")).assunto();

        assertThat(assunto.length()).isLessThan(100);
        assertThat(assunto).doesNotContain("\n", "\r");
    }

    @Test
    @DisplayName("Descrição no tamanho máximo: 1000 caracteres com acentos, sem corte nem reticências")
    void descricaoNoTamanhoMaximo() {
        String descricao = "çãéíõúâ".repeat(142) + "àêôüèì";
        assertThat(descricao.codePointCount(0, descricao.length()))
                .isEqualTo(ValidadorAvaliacao.TAMANHO_MAXIMO_DESCRICAO);

        String corpo = AvisoCritico.de(critica(descricao)).corpo();

        assertThat(corpo).endsWith("Descrição:\n" + descricao);
        assertThat(corpo).doesNotContain("...", "…");
    }

    @Test
    @DisplayName("Caractere fora do plano básico não reduz o limite da descrição")
    void descricaoComEmoji() {
        String descricao = "😀".repeat(ValidadorAvaliacao.TAMANHO_MAXIMO_DESCRICAO);

        assertThat(AvisoCritico.de(critica(descricao)).corpo()).endsWith("\n" + descricao);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "  espaços nas pontas  ",
        "primeira linha\nsegunda linha\n",
        "100% %s %d <b>sem HTML</b> $1"
    })
    @DisplayName("A descrição vai exatamente como foi persistida")
    void descricaoSemAlteracao(String descricao) {
        assertThat(AvisoCritico.de(critica(descricao)).corpo()).endsWith("Descrição:\n" + descricao);
    }

    @Test
    @DisplayName("Data de envio vem da avaliação, não do processamento nem de criadoEm")
    void dataDeEnvioPersistida() {
        Avaliacao avaliacao =
                new Avaliacao("id-1", "Aula ruim", 2, Urgencia.CRITICA, "2020-02-29", "2020-03-01T02:59:59.000Z");
        String hoje = CarimboDeTempo.dataEnvio(Instant.now());

        String corpo = AvisoCritico.de(avaliacao).corpo();

        assertThat(corpo).contains("Data de envio: 2020-02-29");
        assertThat(corpo).doesNotContain(hoje, "2020-03-01");
    }

    @Test
    @DisplayName("Reprocessar a mesma avaliação gera o mesmo aviso")
    void avisoDeterministico() {
        Avaliacao avaliacao = critica("Aula ruim");

        assertThat(AvisoCritico.de(avaliacao)).isEqualTo(AvisoCritico.de(avaliacao));
    }

    @ParameterizedTest
    @EnumSource(value = Urgencia.class, names = "CRITICA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Avaliação que não é crítica não gera aviso")
    void urgenciaDiferenteDeCritica(Urgencia urgencia) {
        Avaliacao avaliacao = new Avaliacao("id-1", "marcador-aluno", 8, urgencia, DATA_ENVIO, CRIADO_EM);

        assertThatThrownBy(() -> AvisoCritico.de(avaliacao))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("marcador-aluno");
    }

    @Test
    @DisplayName("Avaliação nula é rejeitada")
    void avaliacaoNula() {
        assertThatNullPointerException().isThrownBy(() -> AvisoCritico.de(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Construtor rejeita assunto sem texto")
    void construtorRejeitaAssuntoSemTexto(String assunto) {
        assertThatThrownBy(() -> new AvisoCritico(assunto, "corpo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Campo inválido: assunto");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Construtor rejeita corpo sem texto")
    void construtorRejeitaCorpoSemTexto(String corpo) {
        assertThatThrownBy(() -> new AvisoCritico("assunto", corpo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Campo inválido: corpo");
    }

    @Test
    @DisplayName("toString não expõe a descrição")
    void toStringSemDescricao() {
        String texto = AvisoCritico.de(critica("marcador-aluno")).toString();

        assertThat(texto).doesNotContain("marcador-aluno");
    }
}
