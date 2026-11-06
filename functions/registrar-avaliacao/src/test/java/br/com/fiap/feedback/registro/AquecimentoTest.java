package br.com.fiap.feedback.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.registro.LogsRegistrarAvaliacaoTest.AppenderEmMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Hook executado antes do snapshot, com cliente simulado e o layout real de log. */
class AquecimentoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TABELA = "feedback-avaliacoes-dev";
    private static final String MARCADOR = "marcador-aquecimento-4d2b";

    private DynamoDbClient cliente;
    private Aquecimento aquecimento;
    private AppenderEmMemoria appender;
    private Level nivelOriginal;

    @BeforeEach
    void preparar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        Configuration configuracao = contextoLog.getConfiguration();
        nivelOriginal = configuracao.getRootLogger().getLevel();
        appender = new AppenderEmMemoria(configuracao.getAppender("SaidaJson").getLayout());
        appender.start();
        configuracao.getRootLogger().addAppender(appender, Level.ALL, null);
        Configurator.setRootLevel(Level.ALL);

        cliente = mock(DynamoDbClient.class);
        aquecimento = new Aquecimento(cliente, TABELA);
    }

    @AfterEach
    void limpar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        contextoLog.getConfiguration().getRootLogger().removeAppender(appender.getName());
        Configurator.setRootLevel(nivelOriginal);
        appender.stop();
    }

    private List<JsonNode> logs() throws IOException {
        List<JsonNode> logs = new ArrayList<>();
        for (String linha : appender.linhas) {
            logs.add(JSON.readTree(linha));
        }
        return logs;
    }

    private JsonNode unicoLog() throws IOException {
        List<JsonNode> logs = logs();
        assertThat(logs).hasSize(1);
        return logs.getFirst();
    }

    private void assertFalhaRegistradaSemDetalhe(JsonNode log, Class<?> classe) {
        assertThat(log.get("level").textValue()).isEqualTo("WARN");
        assertThat(log.get("message").textValue()).isEqualTo("Falha no aquecimento");
        assertThat(log.get("servico").textValue()).isEqualTo("DynamoDB");
        assertThat(log.get("classe").textValue()).isEqualTo(classe.getName());
        assertThat(log.has("error")).as("pilha ou mensagem da exceção em %s", log).isFalse();
        assertThat(log.has("alarme")).as("marcador de alarme em %s", log).isFalse();
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR).doesNotContain("FALHA_FUNCAO");
    }

    @Test
    @DisplayName("O hook chama DescribeTable na tabela do ambiente e nenhuma outra operação")
    void chamaSoDescribeTable() {
        aquecimento.beforeCheckpoint(null);

        ArgumentCaptor<DescribeTableRequest> captor = ArgumentCaptor.forClass(DescribeTableRequest.class);
        verify(cliente).describeTable(captor.capture());
        assertThat(captor.getValue().tableName()).isEqualTo(TABELA);
        verifyNoMoreInteractions(cliente);
    }

    @Test
    @DisplayName("Sucesso registra em INFO serviço, operação e duração, sem WARN nem ERROR")
    void sucessoRegistraClienteAquecido() throws Exception {
        aquecimento.beforeCheckpoint(null);

        JsonNode log = unicoLog();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("message").textValue()).isEqualTo("Cliente aquecido");
        assertThat(log.get("servico").textValue()).isEqualTo("DynamoDB");
        assertThat(log.get("operacao").textValue()).isEqualTo("DescribeTable");
        assertThat(log.get("duracaoMs").isIntegralNumber()).isTrue();
    }

    @Test
    @DisplayName("Falha do serviço não propaga e registra serviço, classe e código, sem a mensagem")
    void falhaDoServico() throws Exception {
        when(cliente.describeTable(any(DescribeTableRequest.class))).thenThrow(DynamoDbException.builder()
                .message("Negado " + MARCADOR)
                .statusCode(400)
                .requestId("req-1")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDeniedException")
                        .errorMessage("Negado " + MARCADOR)
                        .build())
                .build());

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        JsonNode log = unicoLog();
        assertFalhaRegistradaSemDetalhe(log, DynamoDbException.class);
        assertThat(log.get("codigoErro").textValue()).isEqualTo("AccessDeniedException");
        assertThat(log.has("idRequisicao")).isFalse();
    }

    @Test
    @DisplayName("Falha sem resposta do serviço não propaga e registra só serviço e classe")
    void falhaDoCliente() throws Exception {
        when(cliente.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(SdkClientException.create("Sem rede " + MARCADOR));

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        JsonNode log = unicoLog();
        assertFalhaRegistradaSemDetalhe(log, SdkClientException.class);
        assertThat(log.has("codigoErro")).isFalse();
    }

    @Test
    @DisplayName("LinkageError do cliente não propaga e é registrado só pela classe")
    void linkageError() throws Exception {
        when(cliente.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(new NoClassDefFoundError("classe/" + MARCADOR));

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        JsonNode log = unicoLog();
        assertFalhaRegistradaSemDetalhe(log, NoClassDefFoundError.class);
        assertThat(log.has("codigoErro")).isFalse();
    }

    @Test
    @DisplayName("Depois da restauração o hook não chama o cliente")
    void afterRestoreNaoChama() {
        aquecimento.afterRestore(null);

        verifyNoInteractions(cliente);
        assertThat(appender.linhas).isEmpty();
    }
}
