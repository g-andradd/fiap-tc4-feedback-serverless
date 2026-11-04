package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AvaliacaoTest {

    private static final Instant INSTANTE = Instant.parse("2026-11-10T02:59:59Z");

    private static Map<String, String> itemCompleto() {
        Map<String, String> item = new HashMap<>();
        item.put(AtributosAvaliacao.ID, "abc-123");
        item.put(AtributosAvaliacao.DESCRICAO, "  Aula muito boa  ");
        item.put(AtributosAvaliacao.NOTA, "9");
        item.put(AtributosAvaliacao.URGENCIA, "BAIXA");
        item.put(AtributosAvaliacao.DATA_ENVIO, "2026-11-09");
        item.put(AtributosAvaliacao.CRIADO_EM, "2026-11-10T02:59:59.000Z");
        return item;
    }

    @Test
    void novaCalculaUrgenciaEDatasMantendoADescricaoComoRecebida() {
        Avaliacao avaliacao = Avaliacao.nova("  boa aula  ", 2, "id-1", INSTANTE);

        assertThat(avaliacao.id()).isEqualTo("id-1");
        assertThat(avaliacao.descricao()).isEqualTo("  boa aula  ");
        assertThat(avaliacao.nota()).isEqualTo(2);
        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(avaliacao.dataEnvio()).isEqualTo("2026-11-09");
        assertThat(avaliacao.criadoEm()).isEqualTo("2026-11-10T02:59:59.000Z");
    }

    @Test
    void deAtributosReconstroiUmItemCompleto() {
        assertThat(Avaliacao.deAtributos(itemCompleto())).isEqualTo(new Avaliacao(
                "abc-123",
                "  Aula muito boa  ",
                9,
                Urgencia.BAIXA,
                "2026-11-09",
                "2026-11-10T02:59:59.000Z"));
    }

    @Test
    void deAtributosIgnoraAtributosExtras() {
        Map<String, String> item = itemCompleto();
        item.put("outro", "valor");

        assertThat(Avaliacao.deAtributos(item).id()).isEqualTo("abc-123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "descricao", "nota", "urgencia", "dataEnvio", "criadoEm"})
    void deAtributosApontaOCampoFaltante(String campo) {
        Map<String, String> item = itemCompleto();
        item.remove(campo);

        assertThatThrownBy(() -> Avaliacao.deAtributos(item))
                .isInstanceOfSatisfying(ItemInvalidoException.class,
                        e -> assertThat(e.campos()).containsExactly(campo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "descricao", "dataEnvio", "criadoEm"})
    void deAtributosTrataTextoVazioComoFaltante(String campo) {
        Map<String, String> item = itemCompleto();
        item.put(campo, "");

        assertThatThrownBy(() -> Avaliacao.deAtributos(item))
                .isInstanceOfSatisfying(ItemInvalidoException.class,
                        e -> assertThat(e.campos()).containsExactly(campo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nove", "9.5", "", "11", "-1", "99999999999999999999"})
    void deAtributosRejeitaNotaInvalida(String nota) {
        Map<String, String> item = itemCompleto();
        item.put(AtributosAvaliacao.NOTA, nota);

        assertThatThrownBy(() -> Avaliacao.deAtributos(item))
                .isInstanceOfSatisfying(ItemInvalidoException.class,
                        e -> assertThat(e.campos()).containsExactly(AtributosAvaliacao.NOTA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"URGENTE", "critica", ""})
    void deAtributosRejeitaUrgenciaDesconhecida(String urgencia) {
        Map<String, String> item = itemCompleto();
        item.put(AtributosAvaliacao.URGENCIA, urgencia);

        assertThatThrownBy(() -> Avaliacao.deAtributos(item))
                .isInstanceOfSatisfying(ItemInvalidoException.class,
                        e -> assertThat(e.campos()).containsExactly(AtributosAvaliacao.URGENCIA));
    }

    @Test
    void mensagemDaExcecaoCitaOsCamposSemOsValores() {
        Map<String, String> item = itemCompleto();
        item.put(AtributosAvaliacao.DESCRICAO, " ");
        item.put(AtributosAvaliacao.NOTA, "valor-secreto-nota");
        item.put(AtributosAvaliacao.URGENCIA, "valor-secreto-urgencia");
        item.remove(AtributosAvaliacao.DATA_ENVIO);

        ItemInvalidoException excecao =
                catchThrowableOfType(ItemInvalidoException.class, () -> Avaliacao.deAtributos(item));

        assertThat(excecao.campos()).containsExactly(
                AtributosAvaliacao.DESCRICAO,
                AtributosAvaliacao.NOTA,
                AtributosAvaliacao.URGENCIA,
                AtributosAvaliacao.DATA_ENVIO);
        assertThat(excecao).hasMessageContaining("nota").hasMessageContaining("urgencia");
        assertThat(excecao.getMessage())
                .doesNotContain("valor-secreto-nota")
                .doesNotContain("valor-secreto-urgencia")
                .doesNotContain("abc-123");
        assertThat(excecao).hasNoCause();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
        "NULO, Aula boa, 5, MEDIA, 2026-11-09, 2026-11-10T02:59:59.000Z",
        "id, NULO, 5, MEDIA, 2026-11-09, 2026-11-10T02:59:59.000Z",
        "id, Aula boa, 11, BAIXA, 2026-11-09, 2026-11-10T02:59:59.000Z",
        "id, Aula boa, -1, CRITICA, 2026-11-09, 2026-11-10T02:59:59.000Z",
        "id, Aula boa, 5, NULO, 2026-11-09, 2026-11-10T02:59:59.000Z",
        "id, Aula boa, 5, MEDIA, NULO, 2026-11-10T02:59:59.000Z",
        "id, Aula boa, 5, MEDIA, 2026-11-09, NULO"
    })
    void construtorRejeitaAtributoForaDoContrato(
            String id, String descricao, int nota, Urgencia urgencia, String dataEnvio, String criadoEm) {
        assertThatThrownBy(() -> new Avaliacao(id, descricao, nota, urgencia, dataEnvio, criadoEm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Campo inválido");
    }

    @Test
    void construtorRejeitaDescricaoLongaSemCitarOValor() {
        String longa = "marcador".repeat(200);

        assertThatThrownBy(() -> new Avaliacao("id", longa, 5, Urgencia.MEDIA, "2026-11-09", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Campo inválido: descricao");
    }

    @Test
    void toStringNaoExpoeDescricaoNemNota() {
        String texto = Avaliacao.nova("marcador-do-aluno", 8, "id-1", INSTANTE).toString();

        assertThat(texto)
                .contains("id-1", "BAIXA")
                .doesNotContain("marcador-do-aluno")
                .doesNotContain("nota");
    }
}
