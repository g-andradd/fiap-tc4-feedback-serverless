package br.com.fiap.feedback.registro;

import br.com.fiap.feedback.dominio.AtributosAvaliacao;
import br.com.fiap.feedback.dominio.ValorEntrada;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Base64;

/**
 * Converte o corpo da requisição nos dois valores que o domínio valida. Campos extras não são lidos.
 */
final class LeitorCorpo {

    /** Valores de {@code descricao} e {@code nota} encontrados no corpo. */
    record EntradaAvaliacao(ValorEntrada descricao, ValorEntrada nota) {
    }

    private static final ObjectMapper LEITOR = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();

    private LeitorCorpo() {
    }

    static EntradaAvaliacao ler(String corpo, boolean emBase64) throws CorpoInvalidoException {
        if (corpo == null || corpo.isEmpty()) {
            throw new CorpoInvalidoException("CorpoAusente");
        }
        JsonNode raiz = emBase64 ? interpretar(decodificar(corpo)) : interpretar(corpo);
        if (!raiz.isObject()) {
            throw new CorpoInvalidoException("CorpoNaoEhObjeto");
        }
        return new EntradaAvaliacao(
                converter(raiz.get(AtributosAvaliacao.DESCRICAO)),
                converter(raiz.get(AtributosAvaliacao.NOTA)));
    }

    private static byte[] decodificar(String corpo) throws CorpoInvalidoException {
        try {
            return Base64.getDecoder().decode(corpo);
        } catch (IllegalArgumentException e) {
            throw new CorpoInvalidoException("Base64Invalido");
        }
    }

    private static JsonNode interpretar(String corpo) throws CorpoInvalidoException {
        try {
            return LEITOR.readTree(corpo);
        } catch (IOException e) {
            throw new CorpoInvalidoException(e.getClass().getName());
        }
    }

    private static JsonNode interpretar(byte[] corpo) throws CorpoInvalidoException {
        try {
            return LEITOR.readTree(corpo);
        } catch (IOException e) {
            throw new CorpoInvalidoException(e.getClass().getName());
        }
    }

    private static ValorEntrada converter(JsonNode no) {
        if (no == null || no.isNull()) {
            return new ValorEntrada.Ausente();
        }
        if (no.isTextual()) {
            return new ValorEntrada.Texto(no.textValue());
        }
        if (no.isIntegralNumber()) {
            return new ValorEntrada.Inteiro(no.bigIntegerValue());
        }
        return new ValorEntrada.Outro();
    }
}
