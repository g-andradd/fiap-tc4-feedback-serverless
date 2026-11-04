package br.com.fiap.feedback.registro;

import static br.com.fiap.feedback.registro.ApoioTeste.contexto;
import static br.com.fiap.feedback.registro.ApoioTeste.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.registro.ApoioTeste.RastreadorFalso;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.lambda.powertools.metrics.Metrics;

/**
 * Captura os logs com o layout real do {@code log4j2.xml} e o nível mais baixo possível, para que a
 * ausência do dado valha em todos os níveis, inclusive nos campos de exceção do layout.
 */
class LogsRegistrarAvaliacaoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MARCADOR = "marcador-unico-8f3a";
    private static final String NOTA_INVALIDA = "987654";

    private DynamoDbClient cliente;
    private RegistrarAvaliacaoHandler handler;
    private AppenderEmMemoria appender;
    private Level nivelOriginal;

    @BeforeEach
    void preparar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        Configuration configuracao = contextoLog.getConfiguration();
        nivelOriginal = configuracao.getRootLogger().getLevel();

        appender = new AppenderEmMemoria(layoutReal(configuracao));
        appender.start();
        configuracao.getRootLogger().addAppender(appender, Level.ALL, null);
        Configurator.setRootLevel(Level.ALL);

        cliente = mock(DynamoDbClient.class);
        handler = new RegistrarAvaliacaoHandler(
                cliente,
                "tabela-teste",
                Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC),
                mock(Metrics.class),
                new RastreadorFalso());
    }

    @AfterEach
    void limpar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        contextoLog.getConfiguration().getRootLogger().removeAppender(appender.getName());
        Configurator.setRootLevel(nivelOriginal);
        appender.stop();
    }

    private static Layout<? extends Serializable> layoutReal(Configuration configuracao) {
        Appender console = configuracao.getAppender("SaidaJson");
        return console.getLayout();
    }

    private APIGatewayV2HTTPResponse enviar(String corpo) {
        return handler.handleRequest(evento(corpo), contexto());
    }

    private List<JsonNode> logsEmJson() throws IOException {
        List<JsonNode> logs = new ArrayList<>();
        for (String linha : appender.linhas) {
            logs.add(JSON.readTree(linha));
        }
        return logs;
    }

    private void assertSemDadoDoAluno() {
        assertThat(appender.linhas).isNotEmpty();
        assertThat(String.join("\n", appender.linhas))
                .doesNotContain(MARCADOR)
                .doesNotContain(NOTA_INVALIDA);
    }

    private void assertSemAtributoNota() throws IOException {
        for (JsonNode log : logsEmJson()) {
            assertThat(log.has("nota")).as("atributo nota em %s", log).isFalse();
            assertThat(log.has("descricao")).as("atributo descricao em %s", log).isFalse();
        }
    }

    private JsonNode unicoLog() throws IOException {
        List<JsonNode> logs = logsEmJson();
        assertThat(logs).hasSize(1);
        return logs.getFirst();
    }

    @Test
    @DisplayName("Registro bem-sucedido não registra marcador nem atributo com a nota")
    void registroBemSucedido() throws Exception {
        APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":7}");

        assertThat(resposta.getStatusCode()).isEqualTo(201);
        assertSemDadoDoAluno();
        assertSemAtributoNota();
    }

    @Test
    @DisplayName("Registro bem-sucedido registra id e urgência, que o sistema gera")
    void sucessoRegistraIdEUrgencia() throws Exception {
        APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":7}");

        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("id").textValue()).isEqualTo(JSON.readTree(resposta.getBody()).get("id").textValue());
        assertThat(log.get("urgencia").textValue()).isEqualTo("BAIXA");
    }

    @Test
    @DisplayName("Entrada rejeitada não registra marcador nem o valor da nota")
    void entradaRejeitada() throws Exception {
        APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":" + NOTA_INVALIDA + "}");

        assertThat(resposta.getStatusCode()).isEqualTo(400);
        assertSemDadoDoAluno();
        assertSemAtributoNota();
    }

    @Test
    @DisplayName("Entrada rejeitada registra os nomes dos campos inválidos")
    void rejeicaoRegistraNomesDosCampos() throws Exception {
        enviar("{\"descricao\":\"\",\"nota\":" + NOTA_INVALIDA + "}");

        assertThat(unicoLog().get("campos").toString()).contains("descricao", "nota");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"descricao\": \"" + MARCADOR + "\", \"nota\": 987654",
        "[\"" + MARCADOR + "\", 987654]",
        "{\"descricao\": \"" + MARCADOR + "\", \"nota\": 987654} " + MARCADOR,
        MARCADOR,
        "\"" + MARCADOR + "\""
    })
    @DisplayName("Corpo malformado não registra o marcador")
    void corpoMalformado(String corpo) throws Exception {
        assertThat(enviar(corpo).getStatusCode()).isEqualTo(400);

        assertSemDadoDoAluno();
    }

    @Test
    @DisplayName("Exceção do Jackson é registrada só pela classe")
    void excecaoDoJacksonSoPelaClasse() throws Exception {
        enviar("{\"descricao\": \"" + MARCADOR + "\", \"nota\": 987654");

        JsonNode log = unicoLog();
        assertThat(log.get("motivo").textValue()).startsWith("com.fasterxml.jackson.");
        assertThat(log.has("error")).isFalse();
    }

    @Test
    @DisplayName("Base64 malformado não registra o marcador")
    void base64Malformado() throws Exception {
        String corpo = Base64.getEncoder()
                .encodeToString(("{\"descricao\":\"" + MARCADOR + "\"").getBytes(StandardCharsets.UTF_8));

        APIGatewayV2HTTPEvent evento =
                APIGatewayV2HTTPEvent.builder().withBody(corpo).withIsBase64Encoded(true).build();

        assertThat(handler.handleRequest(evento, contexto()).getStatusCode()).isEqualTo(400);
        assertSemDadoDoAluno();
    }

    private static String emBase64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static Stream<Arguments> caminhosDeCorpoInvalido() {
        String jacksonTruncado = "{\"descricao\": \"" + MARCADOR + "\", \"nota\": 987654";
        String naoObjeto = "[\"" + MARCADOR + "\", 987654]";
        String classeJackson = "com\\.fasterxml\\.jackson\\.[\\w.$]+";
        return Stream.of(
                Arguments.of(null, false, "CorpoAusente"),
                Arguments.of("", false, "CorpoAusente"),
                Arguments.of(null, true, "CorpoAusente"),
                Arguments.of("", true, "CorpoAusente"),
                Arguments.of(naoObjeto, false, "CorpoNaoEhObjeto"),
                Arguments.of(emBase64(naoObjeto), true, "CorpoNaoEhObjeto"),
                Arguments.of(jacksonTruncado, false, classeJackson),
                Arguments.of(MARCADOR, false, classeJackson),
                Arguments.of(emBase64(jacksonTruncado), true, classeJackson),
                Arguments.of(emBase64(MARCADOR), true, classeJackson),
                Arguments.of(MARCADOR + " 987654!", true, "Base64Invalido"));
    }

    @ParameterizedTest
    @MethodSource("caminhosDeCorpoInvalido")
    @DisplayName("Todo caminho de corpo inválido registra um motivo fixo ou o nome de uma classe")
    void motivoNaoTemDadoDeEntrada(String corpo, boolean emBase64, String motivoEsperado) throws Exception {
        APIGatewayV2HTTPEvent evento =
                APIGatewayV2HTTPEvent.builder().withBody(corpo).withIsBase64Encoded(emBase64).build();

        assertThat(handler.handleRequest(evento, contexto()).getStatusCode()).isEqualTo(400);

        assertSemDadoDoAluno();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("motivo").textValue()).matches(motivoEsperado);
        assertThat(log.has("error")).isFalse();
    }

    @Test
    @DisplayName("Falha do serviço registra classe, código e ID da requisição, sem a mensagem")
    void falhaDoServico() throws Exception {
        when(cliente.putItem(any(PutItemRequest.class))).thenThrow(DynamoDbException.builder()
                .message("Falhou com " + MARCADOR + " e " + NOTA_INVALIDA)
                .requestId("req-1")
                .statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("ValidationException")
                        .errorMessage("Falhou com " + MARCADOR)
                        .build())
                .build());

        assertThat(enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":7}").getStatusCode()).isEqualTo(500);

        assertSemDadoDoAluno();
        assertSemAtributoNota();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("classe").textValue()).isEqualTo(DynamoDbException.class.getName());
        assertThat(log.get("codigoErro").textValue()).isEqualTo("ValidationException");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("req-1");
        assertThat(log.has("error")).isFalse();
    }

    @Test
    @DisplayName("Falha do serviço sem detalhes de erro não quebra o log")
    void falhaDoServicoSemDetalhes() throws Exception {
        when(cliente.putItem(any(PutItemRequest.class))).thenThrow(DynamoDbException.builder()
                .message("Falhou com " + MARCADOR)
                .build());

        assertThat(enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":7}").getStatusCode()).isEqualTo(500);

        assertSemDadoDoAluno();
        JsonNode log = unicoLog();
        assertThat(log.get("codigoErro").textValue()).isEqualTo("desconhecido");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("desconhecido");
    }

    @Test
    @DisplayName("Falha sem resposta do serviço registra só a classe")
    void falhaDoCliente() throws Exception {
        when(cliente.putItem(any(PutItemRequest.class)))
                .thenThrow(SdkClientException.create("Falhou com " + MARCADOR));

        assertThat(enviar("{\"descricao\":\"" + MARCADOR + "\",\"nota\":7}").getStatusCode()).isEqualTo(500);

        assertSemDadoDoAluno();
        assertThat(unicoLog().get("classe").textValue()).isEqualTo(SdkClientException.class.getName());
    }

    @Test
    @DisplayName("Toda linha de log é JSON, com os campos do layout do Powertools")
    void logsSaemEmJsonComContextoDaLambda() throws Exception {
        enviar("{\"descricao\":\"Aula boa\",\"nota\":7}");

        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("message").textValue()).isEqualTo("Avaliação registrada");
        assertThat(log.get("function_name").textValue()).isEqualTo("funcao-teste");
        assertThat(log.get("function_request_id").textValue()).isEqualTo("requisicao-teste");
        assertThat(log.has("timestamp")).isTrue();
        assertThat(log.has("service")).isTrue();
    }

    @Test
    @DisplayName("O layout JSON vem da dependência e é o usado pelo appender")
    void layoutJsonExisteNaDependencia() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);

        assertThat(getClass().getClassLoader().getResource("LambdaJsonLayout.json")).isNotNull();
        assertThat(layoutReal(contextoLog.getConfiguration())).isInstanceOf(JsonTemplateLayout.class);
    }

    // Prova de que a captura enxerga todos os níveis e o texto de exceções. Sem isso, as verificações
    // de ausência acima passariam mesmo com o appender quebrado.
    @Test
    @DisplayName("A captura enxerga todos os níveis e o texto de exceções")
    void capturaFunciona() {
        Logger log = LoggerFactory.getLogger(RegistrarAvaliacaoHandler.class);

        log.trace("rastro-" + MARCADOR);
        log.debug("depuracao-" + MARCADOR);
        log.error("erro", new IllegalStateException("excecao-" + MARCADOR));

        assertThat(String.join("\n", appender.linhas))
                .contains("rastro-" + MARCADOR, "depuracao-" + MARCADOR, "excecao-" + MARCADOR);
    }

    static final class AppenderEmMemoria extends AbstractAppender {
        final List<String> linhas = Collections.synchronizedList(new ArrayList<>());

        AppenderEmMemoria(Layout<? extends Serializable> layout) {
            super("EmMemoria", null, layout, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent evento) {
            linhas.add(new String(getLayout().toByteArray(evento), StandardCharsets.UTF_8));
        }
    }
}
