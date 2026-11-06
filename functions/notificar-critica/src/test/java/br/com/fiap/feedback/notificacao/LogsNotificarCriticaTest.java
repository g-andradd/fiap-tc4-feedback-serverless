package br.com.fiap.feedback.notificacao;

import static br.com.fiap.feedback.notificacao.ApoioTeste.contexto;
import static br.com.fiap.feedback.notificacao.ApoioTeste.insercao;
import static br.com.fiap.feedback.notificacao.ApoioTeste.itemCritico;
import static br.com.fiap.feedback.notificacao.ApoioTeste.lote;
import static br.com.fiap.feedback.notificacao.ApoioTeste.registro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.notificacao.ApoioTeste.RastreadorFalso;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.lambda.powertools.metrics.Metrics;

/**
 * Captura os logs com o layout real do {@code log4j2.xml} e o nível mais baixo possível, para que a
 * ausência da descrição valha em todos os níveis, inclusive nos campos de exceção do layout.
 */
class LogsNotificarCriticaTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MARCADOR = "marcador-unico-7c1e";
    // O filtro de métrica busca por substring, por isso a ausência é conferida no texto da linha.
    private static final String MARCADOR_ALARME = "FALHA_FUNCAO";

    private SnsClient cliente;
    private NotificarCriticaHandler handler;
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

        cliente = mock(SnsClient.class);
        handler = new NotificarCriticaHandler(cliente, "arn-topico", mock(Metrics.class), new RastreadorFalso());
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

    private StreamsEventResponse processar(DynamodbEvent evento) {
        return handler.handleRequest(evento, contexto());
    }

    private void assertSemDescricao() {
        assertThat(appender.linhas).isNotEmpty();
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR);
    }

    private JsonNode unicoLog() throws IOException {
        assertThat(appender.linhas).hasSize(1);
        return JSON.readTree(appender.linhas.getFirst());
    }

    private List<JsonNode> logsEmJson() throws IOException {
        List<JsonNode> logs = new ArrayList<>();
        for (String linha : appender.linhas) {
            logs.add(JSON.readTree(linha));
        }
        return logs;
    }

    private void assertSemWarnNemErrorNemMarcador() throws IOException {
        assertThat(logsEmJson())
                .extracting(log -> log.get("level").textValue())
                .isNotEmpty()
                .doesNotContain("WARN", "ERROR");
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR_ALARME);
    }

    private static void assertComMarcadorDeAlarme(JsonNode log) {
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("alarme").textValue()).isEqualTo(MARCADOR_ALARME);
    }

    private void falharPublicacaoComMarcador() {
        when(cliente.publish(any(PublishRequest.class))).thenThrow(SnsException.builder()
                .message("Falhou com " + MARCADOR)
                .requestId("req-1")
                .statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidParameter")
                        .errorMessage("Falhou com " + MARCADOR)
                        .build())
                .build());
    }

    @Test
    @DisplayName("Processamento com sucesso não registra o marcador, em nível nenhum")
    void sucesso() throws Exception {
        when(cliente.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("mensagem-1").build());

        processar(lote(insercao("100", itemCritico("id-1", MARCADOR))));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("message").textValue()).isEqualTo("Aviso publicado");
        assertThat(log.get("id").textValue()).isEqualTo("id-1");
        assertThat(log.get("eventID").textValue()).isEqualTo("evento-100");
        assertThat(log.get("messageId").textValue()).isEqualTo("mensagem-1");
    }

    @Test
    @DisplayName("Falha ao publicar registra em ERROR, sem o marcador nem a exceção")
    void falhaDoServico() throws Exception {
        falharPublicacaoComMarcador();

        processar(lote(insercao("100", itemCritico("id-1", MARCADOR))));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("message").textValue()).isEqualTo("Falha ao publicar aviso");
        assertThat(log.get("id").textValue()).isEqualTo("id-1");
        assertThat(log.get("eventID").textValue()).isEqualTo("evento-100");
        assertThat(log.get("sequencia").textValue()).isEqualTo("100");
        assertThat(log.get("classe").textValue()).isEqualTo(SnsException.class.getName());
        assertThat(log.get("codigoErro").textValue()).isEqualTo("InvalidParameter");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("req-1");
        assertThat(log.has("error")).isFalse();
        assertComMarcadorDeAlarme(log);
    }

    @Test
    @DisplayName("Falha do serviço sem detalhes de erro não quebra o log")
    void falhaDoServicoSemDetalhes() throws Exception {
        when(cliente.publish(any(PublishRequest.class))).thenThrow(SnsException.builder()
                .message("Falhou com " + MARCADOR)
                .build());

        processar(lote(insercao("100", itemCritico("id-1", MARCADOR))));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("codigoErro").textValue()).isEqualTo("desconhecido");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("desconhecido");
    }

    @Test
    @DisplayName("Falha sem resposta do serviço registra só a classe, em ERROR")
    void falhaDoCliente() throws Exception {
        when(cliente.publish(any(PublishRequest.class)))
                .thenThrow(SdkClientException.create("Falhou com " + MARCADOR));

        processar(lote(insercao("100", itemCritico("id-1", MARCADOR))));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("classe").textValue()).isEqualTo(SdkClientException.class.getName());
        assertThat(log.get("codigoErro").textValue()).isEqualTo("desconhecido");
        assertComMarcadorDeAlarme(log);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "nota", "dataEnvio", "criadoEm"})
    @DisplayName("Item inválido registra em ERROR o eventID e os nomes dos campos, sem o marcador")
    void itemInvalido(String campo) throws Exception {
        Map<String, AttributeValue> imagem = itemCritico("id-1", MARCADOR);
        imagem.remove(campo);

        processar(lote(insercao("100", imagem)));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("message").textValue()).isEqualTo("Item inválido descartado");
        assertThat(log.get("eventID").textValue()).isEqualTo("evento-100");
        assertThat(log.get("campos").toString()).contains(campo);
        assertThat(log.has("error")).isFalse();
        assertComMarcadorDeAlarme(log);
    }

    @Test
    @DisplayName("Lote publicado com sucesso não grava linha em WARN ou ERROR nem o marcador de alarme")
    void loteComSucessoSemWarnNemMarcador() throws Exception {
        when(cliente.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("mensagem-1").build());

        processar(lote(
                insercao("100", itemCritico("id-1", "Aula ruim")),
                insercao("101", itemCritico("id-2", "Aula péssima"))));

        assertThat(appender.linhas).hasSize(2);
        assertSemWarnNemErrorNemMarcador();
    }

    @Test
    @DisplayName("Registro ignorado não grava linha em WARN ou ERROR nem o marcador de alarme")
    void ignoradoSemWarnNemMarcador() throws Exception {
        processar(lote(registro("MODIFY", "100", itemCritico("id-1", "Aula"))));

        assertSemWarnNemErrorNemMarcador();
    }

    @Test
    @DisplayName("Falha parcial do lote: só a linha da publicação que falhou leva o marcador")
    void falhaParcialDoLote() throws Exception {
        when(cliente.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("mensagem-1").build())
                .thenThrow(SdkClientException.create("Falhou com " + MARCADOR));

        StreamsEventResponse resposta = processar(lote(
                insercao("100", itemCritico("id-1", MARCADOR)),
                insercao("101", itemCritico("id-2", MARCADOR))));

        assertThat(resposta.getBatchItemFailures()).hasSize(1);
        assertSemDescricao();
        List<JsonNode> logs = logsEmJson();
        assertThat(logs).extracting(log -> log.get("message").textValue())
                .containsExactly("Aviso publicado", "Falha ao publicar aviso");
        assertThat(logs.getFirst().has("alarme")).isFalse();
        assertComMarcadorDeAlarme(logs.getLast());
        assertThat(logs.getLast().get("sequencia").textValue()).isEqualTo("101");
    }

    @Test
    @DisplayName("Error que escapa da invocação é registrado só pela classe, com o marcador, e relançado")
    void errorEscapadoComMarcador() throws Exception {
        NoClassDefFoundError erro = new NoClassDefFoundError("classe/" + MARCADOR);
        when(cliente.publish(any(PublishRequest.class))).thenThrow(erro);

        assertThatThrownBy(() -> processar(lote(insercao("100", itemCritico("id-1", MARCADOR))))).isSameAs(erro);

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertComMarcadorDeAlarme(log);
        assertThat(log.get("message").textValue()).isEqualTo("Erro na execução da função");
        assertThat(log.get("classe").textValue()).isEqualTo(NoClassDefFoundError.class.getName());
        assertThat(log.has("error")).isFalse();
    }

    @Test
    @DisplayName("Descrição longa demais, rejeitada no item, não vai para o log")
    void descricaoLongaDemais() throws Exception {
        processar(lote(insercao("100", itemCritico("id-1", MARCADOR.repeat(100)))));

        assertSemDescricao();
        assertThat(unicoLog().get("campos").toString()).contains("descricao");
    }

    @Test
    @DisplayName("Registro ignorado registra só eventID e eventName, sem o marcador")
    void registroIgnorado() throws Exception {
        processar(lote(registro("MODIFY", "100", itemCritico("id-1", MARCADOR))));

        assertSemDescricao();
        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("message").textValue()).isEqualTo("Registro ignorado");
        assertThat(log.get("eventID").textValue()).isEqualTo("evento-100");
        assertThat(log.get("eventName").textValue()).isEqualTo("MODIFY");
    }

    @Test
    @DisplayName("Toda linha de log é JSON, com os campos do layout do Powertools")
    void logsSaemEmJsonComContextoDaLambda() throws Exception {
        when(cliente.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("mensagem-1").build());

        processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

        JsonNode log = unicoLog();
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
        Logger log = LoggerFactory.getLogger(NotificarCriticaHandler.class);

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
