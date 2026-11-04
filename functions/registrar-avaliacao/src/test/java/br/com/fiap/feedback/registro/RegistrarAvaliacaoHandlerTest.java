package br.com.fiap.feedback.registro;

import static br.com.fiap.feedback.registro.ApoioTeste.contexto;
import static br.com.fiap.feedback.registro.ApoioTeste.evento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.registro.ApoioTeste.RastreadorFalso;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

class RegistrarAvaliacaoHandlerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant INSTANTE = Instant.parse("2026-11-10T02:59:59Z");
    private static final String TABELA = "tabela-teste";
    private static final String EMOJI = "😀";

    private DynamoDbClient cliente;
    private Metrics metricas;
    private RastreadorFalso rastreador;
    private RegistrarAvaliacaoHandler handler;

    @BeforeEach
    void preparar() {
        cliente = mock(DynamoDbClient.class);
        metricas = mock(Metrics.class);
        rastreador = new RastreadorFalso();
        handler = criarHandler(Clock.fixed(INSTANTE, ZoneOffset.UTC));
    }

    private RegistrarAvaliacaoHandler criarHandler(Clock relogio) {
        return new RegistrarAvaliacaoHandler(cliente, TABELA, relogio, metricas, rastreador);
    }

    private APIGatewayV2HTTPResponse enviar(String corpo) {
        return handler.handleRequest(evento(corpo), contexto());
    }

    private static String corpoComNota(String nota) {
        return "{\"descricao\":\"Aula boa\",\"nota\":" + nota + "}";
    }

    private static String corpoComDescricao(String descricaoJson) {
        return "{\"descricao\":" + descricaoJson + ",\"nota\":5}";
    }

    private static String textoJson(String texto) throws Exception {
        return JSON.writeValueAsString(texto);
    }

    private static JsonNode corpoDe(APIGatewayV2HTTPResponse resposta) throws Exception {
        return JSON.readTree(resposta.getBody());
    }

    private static List<String> camposDe(APIGatewayV2HTTPResponse resposta) throws Exception {
        List<String> nomes = new ArrayList<>();
        corpoDe(resposta).get("campos").forEach(no -> nomes.add(no.textValue()));
        return nomes;
    }

    private Map<String, AttributeValue> itemGravado() {
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(cliente).putItem(captor.capture());
        return captor.getValue().item();
    }

    private void assertRejeitadoPor(APIGatewayV2HTTPResponse resposta, String... campos) throws Exception {
        assertThat(resposta.getStatusCode()).isEqualTo(400);
        assertThat(corpoDe(resposta).get("erro").textValue()).isEqualTo("ENTRADA_INVALIDA");
        assertThat(camposDe(resposta)).containsExactly(campos);
        verify(cliente, never()).putItem(any(PutItemRequest.class));
    }

    private void assertCorpoInvalido(APIGatewayV2HTTPResponse resposta) throws Exception {
        assertThat(resposta.getStatusCode()).isEqualTo(400);
        JsonNode corpo = corpoDe(resposta);
        assertThat(corpo.get("erro").textValue()).isEqualTo("CORPO_INVALIDO");
        assertThat(corpo.get("mensagem").textValue())
                .isEqualTo("O corpo da requisição precisa ser um objeto JSON válido.");
        assertThat(corpo.get("campos")).isEmpty();
        verify(cliente, never()).putItem(any(PutItemRequest.class));
    }

    @Nested
    @DisplayName("Registro de avaliação")
    class Registro {

        @Test
        @DisplayName("Avaliação válida registrada")
        void avaliacaoValidaRegistrada() {
            APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\": \"Aula muito boa\", \"nota\": 9}");

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            Map<String, AttributeValue> item = itemGravado();
            assertThat(item.get("descricao").s()).isEqualTo("Aula muito boa");
            assertThat(item.get("nota").n()).isEqualTo("9");
        }

        @Test
        @DisplayName("Grava na tabela configurada um item só com os seis atributos do contrato")
        void gravaOsSeisAtributos() throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("5"));

            ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(cliente).putItem(captor.capture());
            PutItemRequest requisicao = captor.getValue();
            assertThat(requisicao.tableName()).isEqualTo(TABELA);

            Map<String, AttributeValue> item = requisicao.item();
            assertThat(item).containsOnlyKeys("id", "descricao", "nota", "urgencia", "dataEnvio", "criadoEm");
            assertThat(item.get("id").s()).isEqualTo(corpoDe(resposta).get("id").textValue());
            assertThat(item.get("descricao").s()).isEqualTo("Aula boa");
            assertThat(item.get("nota").n()).isEqualTo("5");
            assertThat(item.get("urgencia").s()).isEqualTo("MEDIA");
            assertThat(item.get("dataEnvio").s()).isEqualTo("2026-11-09");
            assertThat(item.get("criadoEm").s()).isEqualTo("2026-11-10T02:59:59.000Z");
        }

        @Test
        @DisplayName("Envios iguais geram avaliações distintas")
        void enviosIguaisGeramAvaliacoesDistintas() throws Exception {
            APIGatewayV2HTTPResponse primeira = enviar(corpoComNota("5"));
            APIGatewayV2HTTPResponse segunda = enviar(corpoComNota("5"));

            assertThat(primeira.getStatusCode()).isEqualTo(201);
            assertThat(segunda.getStatusCode()).isEqualTo(201);
            String primeiroId = corpoDe(primeira).get("id").textValue();
            String segundoId = corpoDe(segunda).get("id").textValue();
            assertThat(primeiroId).isNotEqualTo(segundoId);

            ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(cliente, times(2)).putItem(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(requisicao -> requisicao.item().get("id").s())
                    .containsExactly(primeiroId, segundoId);
        }

        @Test
        @DisplayName("Aceita corpo em base64")
        void aceitaCorpoEmBase64() {
            String corpo = Base64.getEncoder()
                    .encodeToString("{\"descricao\":\"Ótima aula\",\"nota\":8}".getBytes(StandardCharsets.UTF_8));
            APIGatewayV2HTTPEvent evento = APIGatewayV2HTTPEvent.builder()
                    .withBody(corpo)
                    .withIsBase64Encoded(true)
                    .build();

            APIGatewayV2HTTPResponse resposta = handler.handleRequest(evento, contexto());

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(itemGravado().get("descricao").s()).isEqualTo("Ótima aula");
        }
    }

    @Nested
    @DisplayName("Validação de descricao")
    class ValidacaoDescricao {

        @Test
        @DisplayName("Descrição com 1 caractere aceita")
        void umCaractere() {
            assertThat(enviar(corpoComDescricao("\"a\"")).getStatusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("Descrição com 1000 caracteres aceita")
        void milCaracteres() throws Exception {
            assertThat(enviar(corpoComDescricao(textoJson("a".repeat(1000)))).getStatusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("Descrição com 1001 caracteres rejeitada")
        void milEUmCaracteres() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao(textoJson("a".repeat(1001)))), "descricao");
        }

        @Test
        @DisplayName("Caractere fora do plano básico conta como um")
        void milEmojis() throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComDescricao(textoJson(EMOJI.repeat(1000))));

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(itemGravado().get("descricao").s()).isEqualTo(EMOJI.repeat(1000));
        }

        @Test
        @DisplayName("Descrição ausente")
        void ausente() throws Exception {
            assertRejeitadoPor(enviar("{\"nota\":5}"), "descricao");
        }

        @Test
        @DisplayName("Descrição nula")
        void nula() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao("null")), "descricao");
        }

        @Test
        @DisplayName("Descrição vazia")
        void vazia() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao("\"\"")), "descricao");
        }

        @Test
        @DisplayName("Descrição só com espaços")
        void soEspacos() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao("\"   \"")), "descricao");
        }

        @Test
        @DisplayName("Descrição só com tabulação e quebra de linha")
        void soTabulacaoEQuebra() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao("\"\\t\\n\\r\\n\"")), "descricao");
        }

        @Test
        @DisplayName("Descrição que não é texto")
        void naoTexto() throws Exception {
            assertRejeitadoPor(enviar(corpoComDescricao("123")), "descricao");
        }

        @Test
        @DisplayName("Descrição persistida como recebida")
        void persistidaComoRecebida() {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComDescricao("\"  boa aula  \""));

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(itemGravado().get("descricao").s()).isEqualTo("  boa aula  ");
        }
    }

    @Nested
    @DisplayName("Validação de nota")
    class ValidacaoNota {

        @Test
        @DisplayName("Nota mínima aceita")
        void minima() {
            assertThat(enviar(corpoComNota("0")).getStatusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("Nota máxima aceita")
        void maxima() {
            assertThat(enviar(corpoComNota("10")).getStatusCode()).isEqualTo(201);
        }

        @ParameterizedTest(name = "nota {0}")
        @ValueSource(strings = {
            "-1",
            "11",
            "null",
            "7.5",
            "7.0",
            "1e1",
            "\"7\"",
            "99999999999999999999",
            "true",
            "[7]"
        })
        @DisplayName("Nota abaixo do mínimo, acima do máximo, nula, decimal, com expoente, texto e enorme")
        void rejeitada(String nota) throws Exception {
            assertRejeitadoPor(enviar(corpoComNota(nota)), "nota");
        }

        @Test
        @DisplayName("Nota ausente")
        void ausente() throws Exception {
            assertRejeitadoPor(enviar("{\"descricao\":\"Aula boa\"}"), "nota");
        }
    }

    @Nested
    @DisplayName("Rejeição de corpo malformado")
    class CorpoMalformado {

        @Test
        @DisplayName("JSON truncado")
        void truncado() throws Exception {
            assertCorpoInvalido(enviar("{\"descricao\": \"Aula boa\", \"nota\": 5"));
        }

        @Test
        @DisplayName("Corpo vazio")
        void vazio() throws Exception {
            assertCorpoInvalido(enviar(null));
        }

        @Test
        @DisplayName("Corpo com texto vazio")
        void textoVazio() throws Exception {
            assertCorpoInvalido(enviar(""));
        }

        @Test
        @DisplayName("JSON que não é objeto")
        void naoObjeto() throws Exception {
            assertCorpoInvalido(enviar("[1, 2, 3]"));
        }

        @Test
        @DisplayName("Objeto seguido de tokens")
        void tokensSobrando() throws Exception {
            assertCorpoInvalido(enviar("{\"descricao\":\"Aula boa\",\"nota\":5} xx"));
        }
    }

    @Nested
    @DisplayName("Classificação de urgência")
    class Classificacao {

        @ParameterizedTest(name = "Nota {0}")
        @CsvSource({"0, CRITICA", "3, CRITICA", "4, MEDIA", "6, MEDIA", "7, BAIXA", "10, BAIXA"})
        void respostaEItemTrazemAUrgencia(int nota, String urgencia) throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota(Integer.toString(nota)));

            assertThat(corpoDe(resposta).get("urgencia").textValue()).isEqualTo(urgencia);
            assertThat(itemGravado().get("urgencia").s()).isEqualTo(urgencia);
        }

        @Test
        @DisplayName("Urgência informada pelo cliente é ignorada")
        void urgenciaDoClienteIgnorada() throws Exception {
            APIGatewayV2HTTPResponse resposta =
                    enviar("{\"descricao\": \"Aula ruim\", \"nota\": 2, \"urgencia\": \"BAIXA\"}");

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(corpoDe(resposta).get("urgencia").textValue()).isEqualTo("CRITICA");
            assertThat(itemGravado().get("urgencia").s()).isEqualTo("CRITICA");
        }
    }

    @Nested
    @DisplayName("Data de envio atribuída pelo sistema")
    class DataEnvio {

        @Test
        @DisplayName("Último segundo do dia em São Paulo")
        void ultimoSegundo() {
            handler = criarHandler(Clock.fixed(Instant.parse("2026-11-10T02:59:59Z"), ZoneOffset.UTC));

            enviar(corpoComNota("5"));

            Map<String, AttributeValue> item = itemGravado();
            assertThat(item.get("dataEnvio").s()).isEqualTo("2026-11-09");
            assertThat(Instant.parse(item.get("criadoEm").s())).isEqualTo("2026-11-10T02:59:59Z");
        }

        @Test
        @DisplayName("Primeiro segundo do dia em São Paulo")
        void primeiroSegundo() {
            handler = criarHandler(Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC));

            enviar(corpoComNota("5"));

            assertThat(itemGravado().get("dataEnvio").s()).isEqualTo("2026-11-10");
        }

        @Test
        @DisplayName("Data informada pelo cliente é ignorada")
        void dataDoClienteIgnorada() {
            APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\":\"Aula boa\",\"nota\":5,"
                    + "\"dataEnvio\":\"2020-01-01\",\"criadoEm\":\"2020-01-01T00:00:00Z\"}");

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            Map<String, AttributeValue> item = itemGravado();
            assertThat(item.get("dataEnvio").s()).isEqualTo("2026-11-09");
            assertThat(item.get("criadoEm").s()).isEqualTo("2026-11-10T02:59:59.000Z");
        }

        @Test
        @DisplayName("O instante é capturado uma vez por requisição, nunca na inicialização")
        void instanteCapturadoUmaVezPorRequisicao() {
            RelogioQueAvanca relogio = new RelogioQueAvanca(Instant.parse("2026-11-10T02:59:59Z"));
            handler = criarHandler(relogio);
            assertThat(relogio.leituras.get()).isZero();

            enviar(corpoComNota("5"));
            assertThat(relogio.leituras.get()).isEqualTo(1);

            enviar(corpoComNota("5"));
            assertThat(relogio.leituras.get()).isEqualTo(2);

            ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(cliente, times(2)).putItem(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(requisicao -> requisicao.item().get("criadoEm").s())
                    .containsExactly("2026-11-10T02:59:59.000Z", "2026-11-10T03:00:00.000Z");
            assertThat(captor.getAllValues())
                    .extracting(requisicao -> requisicao.item().get("dataEnvio").s())
                    .containsExactly("2026-11-09", "2026-11-10");
        }
    }

    @Nested
    @DisplayName("Identificador atribuído pelo sistema")
    class Identificador {

        @Test
        @DisplayName("Identificador informado pelo cliente é ignorado")
        void idDoClienteIgnorado() throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar("{\"descricao\":\"Aula boa\",\"nota\":5,\"id\":\"abc\"}");

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(corpoDe(resposta).get("id").textValue()).isNotEqualTo("abc");
            assertThat(itemGravado().get("id").s()).isNotEqualTo("abc");
        }

        @Test
        @DisplayName("O identificador é um UUID em texto")
        void idEhUuid() {
            enviar(corpoComNota("5"));

            String id = itemGravado().get("id").s();
            assertThat(UUID.fromString(id)).hasToString(id);
        }
    }

    @Nested
    @DisplayName("Respostas")
    class RespostasHttp {

        @Test
        @DisplayName("Corpo da resposta de sucesso")
        void sucesso() throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("5"));

            assertThat(resposta.getStatusCode()).isEqualTo(201);
            assertThat(resposta.getHeaders()).containsEntry("content-type", "application/json");
            JsonNode corpo = corpoDe(resposta);
            assertThat(corpo.properties()).hasSize(2);
            assertThat(corpo.get("id").textValue()).isEqualTo(itemGravado().get("id").s());
            assertThat(corpo.get("urgencia").textValue()).isEqualTo("MEDIA");
        }

        @Test
        @DisplayName("Um campo inválido")
        void umCampoInvalido() throws Exception {
            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("11"));

            assertThat(resposta.getStatusCode()).isEqualTo(400);
            assertThat(resposta.getHeaders()).containsEntry("content-type", "application/json");
            assertThat(resposta.getBody()).isEqualTo("{\"erro\":\"ENTRADA_INVALIDA\","
                    + "\"mensagem\":\"Um ou mais campos são inválidos.\",\"campos\":[\"nota\"]}");
        }

        @Test
        @DisplayName("Dois campos inválidos")
        void doisCamposInvalidos() throws Exception {
            assertRejeitadoPor(enviar("{\"descricao\":\"\",\"nota\":11}"), "descricao", "nota");
        }

        @Test
        @DisplayName("Corpo inválido segue o formato do design")
        void corpoInvalido() {
            APIGatewayV2HTTPResponse resposta = enviar("[1, 2, 3]");

            assertThat(resposta.getHeaders()).containsEntry("content-type", "application/json");
            assertThat(resposta.getBody()).isEqualTo("{\"erro\":\"CORPO_INVALIDO\","
                    + "\"mensagem\":\"O corpo da requisição precisa ser um objeto JSON válido.\",\"campos\":[]}");
        }
    }

    @Nested
    @DisplayName("Falha interna sem exposição de detalhe")
    class FalhaInterna {

        private static final String CORPO_500 = "{\"erro\":\"ERRO_INTERNO\","
                + "\"mensagem\":\"Não foi possível registrar a avaliação. Tente novamente.\"}";

        @Test
        @DisplayName("Falha ao persistir, erro do serviço")
        void falhaDoServico() {
            when(cliente.putItem(any(PutItemRequest.class))).thenThrow(DynamoDbException.builder()
                    .message("arn:aws:dynamodb:sa-east-1:123456789012:table/feedback-avaliacoes-dev negado")
                    .statusCode(400)
                    .requestId("req-1")
                    .awsErrorDetails(AwsErrorDetails.builder()
                            .errorCode("AccessDeniedException")
                            .errorMessage("arn:aws:dynamodb negado")
                            .build())
                    .build());

            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("5"));

            assertThat(resposta.getStatusCode()).isEqualTo(500);
            assertThat(resposta.getHeaders()).containsEntry("content-type", "application/json");
            assertThat(resposta.getBody())
                    .isEqualTo(CORPO_500)
                    .doesNotContain("arn:")
                    .doesNotContain("Exception")
                    .doesNotContain("feedback-avaliacoes")
                    .doesNotContain("\tat ");
        }

        @Test
        @DisplayName("Falha ao persistir, erro do cliente do SDK")
        void falhaDoCliente() {
            when(cliente.putItem(any(PutItemRequest.class)))
                    .thenThrow(SdkClientException.create("Unable to connect to dynamodb.sa-east-1.amazonaws.com"));

            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("5"));

            assertThat(resposta.getStatusCode()).isEqualTo(500);
            assertThat(resposta.getBody()).isEqualTo(CORPO_500);
        }

        @Test
        @DisplayName("Tempo esgotado no SDK vira 500 sem registrar a mensagem da exceção")
        void tempoEsgotadoNoSdk() throws Exception {
            String marcador = "marcador-tempo-4d1e";
            when(cliente.putItem(any(PutItemRequest.class))).thenThrow(ApiCallTimeoutException.builder()
                    .message("Tempo esgotado com " + marcador)
                    .build());
            LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
            Configuration configuracao = contextoLog.getConfiguration();
            LogsRegistrarAvaliacaoTest.AppenderEmMemoria appender =
                    new LogsRegistrarAvaliacaoTest.AppenderEmMemoria(configuracao.getAppender("SaidaJson").getLayout());
            appender.start();
            configuracao.getRootLogger().addAppender(appender, Level.ALL, null);

            APIGatewayV2HTTPResponse resposta;
            try {
                resposta = enviar(corpoComNota("5"));
            } finally {
                configuracao.getRootLogger().removeAppender(appender.getName());
                appender.stop();
            }

            assertThat(resposta.getStatusCode()).isEqualTo(500);
            assertThat(resposta.getBody()).isEqualTo(CORPO_500);
            assertThat(appender.linhas).hasSize(1);
            assertThat(appender.linhas.getFirst()).doesNotContain(marcador);
            JsonNode log = JSON.readTree(appender.linhas.getFirst());
            assertThat(log.get("level").textValue()).isEqualTo("ERROR");
            assertThat(log.get("classe").textValue()).isEqualTo(ApiCallTimeoutException.class.getName());
            assertThat(log.has("error")).isFalse();
        }

        @Test
        @DisplayName("Falha inesperada no processamento")
        void falhaInesperada() {
            when(cliente.putItem(any(PutItemRequest.class))).thenThrow(new IllegalStateException("detalhe interno"));

            APIGatewayV2HTTPResponse resposta = enviar(corpoComNota("5"));

            assertThat(resposta.getStatusCode()).isEqualTo(500);
            assertThat(resposta.getBody()).isEqualTo(CORPO_500);
        }

        @Test
        @DisplayName("Evento nulo vira 500 sem detalhe")
        void eventoNulo() {
            APIGatewayV2HTTPResponse resposta = handler.handleRequest(null, contexto());

            assertThat(resposta.getStatusCode()).isEqualTo(500);
            assertThat(resposta.getBody()).isEqualTo(CORPO_500);
        }
    }

    @Nested
    @DisplayName("Metricas e rastreamento")
    class MetricasERastreamento {

        @Test
        @DisplayName("Sucesso conta, grava em subsegmento anotado e descarrega no fim")
        void sucesso() {
            Context contexto = contexto();

            handler.handleRequest(evento(corpoComNota("2")), contexto);

            InOrder ordem = inOrder(metricas);
            ordem.verify(metricas).captureColdStartMetric(contexto);
            ordem.verify(metricas).addMetric("AvaliacaoRegistrada", 1, MetricUnit.COUNT);
            ordem.verify(metricas).flush();
            assertThat(rastreador.subsegmentos).containsExactly("gravarAvaliacao");
            assertThat(rastreador.anotacoes).containsOnly(Map.entry("urgencia", "CRITICA"));
        }

        @Test
        @DisplayName("Entrada rejeitada conta e descarrega, sem subsegmento")
        void rejeicao() {
            enviar(corpoComNota("11"));
            enviar("[1, 2, 3]");

            verify(metricas, times(2)).addMetric("EntradaRejeitada", 1, MetricUnit.COUNT);
            verify(metricas, times(2)).flush();
            assertThat(rastreador.subsegmentos).isEmpty();
        }

        @Test
        @DisplayName("Falha ao gravar conta como erro interno e descarrega")
        void falhaAoGravar() {
            when(cliente.putItem(any(PutItemRequest.class))).thenThrow(new IllegalStateException("falha"));

            enviar(corpoComNota("5"));

            verify(metricas).addMetric("ErroInterno", 1, MetricUnit.COUNT);
            verify(metricas, never()).addMetric(eq("AvaliacaoRegistrada"), anyDouble(), any(MetricUnit.class));
            verify(metricas).flush();
        }

        @Test
        @DisplayName("Falha ao descarregar métricas não troca a resposta")
        void falhaNoFlush() {
            doThrow(new IllegalStateException("sem namespace")).when(metricas).flush();

            assertThat(enviar(corpoComNota("5")).getStatusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("Falha ao contar métrica não vira 500 depois de gravar")
        void falhaAoContar() {
            doThrow(new IllegalStateException("sem namespace"))
                    .when(metricas).addMetric(anyString(), anyDouble(), any(MetricUnit.class));

            assertThat(enviar(corpoComNota("5")).getStatusCode()).isEqualTo(201);
            verify(cliente).putItem(any(PutItemRequest.class));
        }

        @Test
        @DisplayName("Falha ao medir cold start não impede o registro")
        void falhaNoColdStart() {
            doThrow(new IllegalStateException("sem namespace")).when(metricas).captureColdStartMetric(any(Context.class));

            assertThat(enviar(corpoComNota("5")).getStatusCode()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Inicialização com SnapStart")
    class Inicializacao {

        @Test
        @DisplayName("Cache de DNS desligado ao carregar a classe")
        void cacheDeDns() {
            assertThat(Security.getProperty("networkaddress.cache.ttl")).isEqualTo("0");
        }

        @Test
        @DisplayName("Runtime encontra o construtor público sem argumentos")
        void construtorPublico() throws Exception {
            assertThat(RegistrarAvaliacaoHandler.class.getConstructor()).isNotNull();
        }

        @Test
        @DisplayName("O cliente desiste da chamada em 8 segundos, abaixo do Timeout da função")
        void tempoMaximoDaChamada() {
            DynamoDbClientBuilder construtor = DynamoDbClient.builder()
                    .region(Region.SA_EAST_1)
                    .credentialsProvider(AnonymousCredentialsProvider.create());

            try (DynamoDbClient clienteReal = RegistrarAvaliacaoHandler.criarCliente(construtor)) {
                assertThat(clienteReal.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(Duration.ofSeconds(8));
            }
        }
    }

    /** Avança um segundo por leitura, para provar que o instante é lido a cada requisição. */
    private static final class RelogioQueAvanca extends Clock {
        final AtomicInteger leituras = new AtomicInteger();
        private final Instant inicio;

        RelogioQueAvanca(Instant inicio) {
            this.inicio = inicio;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return inicio.plusSeconds(leituras.getAndIncrement());
        }
    }
}
