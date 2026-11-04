package br.com.fiap.feedback.registro;

import br.com.fiap.feedback.dominio.Avaliacao;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * Corpos e códigos HTTP da função. {@code campos} traz só nomes, nunca valores.
 */
final class Respostas {

    private static final Map<String, String> CABECALHOS = Map.of("content-type", "application/json");
    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private Respostas() {
    }

    static APIGatewayV2HTTPResponse sucesso(Avaliacao avaliacao) {
        ObjectNode corpo = JSON.objectNode()
                .put("id", avaliacao.id())
                .put("urgencia", avaliacao.urgencia().name());
        return resposta(201, corpo);
    }

    static APIGatewayV2HTTPResponse camposInvalidos(List<String> campos) {
        return erro400("ENTRADA_INVALIDA", "Um ou mais campos são inválidos.", campos);
    }

    static APIGatewayV2HTTPResponse corpoInvalido() {
        return erro400("CORPO_INVALIDO", "O corpo da requisição precisa ser um objeto JSON válido.", List.of());
    }

    static APIGatewayV2HTTPResponse erroInterno() {
        ObjectNode corpo = JSON.objectNode()
                .put("erro", "ERRO_INTERNO")
                .put("mensagem", "Não foi possível registrar a avaliação. Tente novamente.");
        return resposta(500, corpo);
    }

    private static APIGatewayV2HTTPResponse erro400(String erro, String mensagem, List<String> campos) {
        ObjectNode corpo = JSON.objectNode().put("erro", erro).put("mensagem", mensagem);
        ArrayNode nomes = corpo.putArray("campos");
        campos.forEach(nomes::add);
        return resposta(400, corpo);
    }

    private static APIGatewayV2HTTPResponse resposta(int status, ObjectNode corpo) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(CABECALHOS)
                .withBody(corpo.toString())
                .build();
    }
}
