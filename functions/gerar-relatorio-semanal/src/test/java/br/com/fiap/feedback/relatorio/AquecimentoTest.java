package br.com.fiap.feedback.relatorio;

import static br.com.fiap.feedback.relatorio.ApoioTeste.BUCKET;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TABELA;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TOPICO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.relatorio.LogsGerarRelatorioSemanalTest.AppenderEmMemoria;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Hook executado antes do snapshot, com clientes simulados e o layout real de log. */
class AquecimentoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MARCADOR = "marcador-aquecimento-1f7c";

    private DynamoDbClient dynamo;
    private S3Client s3;
    private SnsClient sns;
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

        dynamo = mock(DynamoDbClient.class);
        s3 = mock(S3Client.class);
        sns = mock(SnsClient.class);
        aquecimento = new Aquecimento(dynamo, TABELA, s3, BUCKET, sns, TOPICO);
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

    private JsonNode logDoServico(String servico) throws IOException {
        List<JsonNode> doServico = logs().stream()
                .filter(log -> servico.equals(log.path("servico").textValue()))
                .toList();
        assertThat(doServico).hasSize(1);
        return doServico.getFirst();
    }

    private void falharCliente(String servico, Throwable falha) {
        switch (servico) {
            case "DynamoDB" -> when(dynamo.query(any(QueryRequest.class))).thenThrow(falha);
            case "S3" -> when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(falha);
            case "SNS" -> when(sns.getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(falha);
            default -> throw new IllegalArgumentException(servico);
        }
    }

    private void assertTresClientesChamados() {
        verify(dynamo).query(any(QueryRequest.class));
        verify(s3).headBucket(any(HeadBucketRequest.class));
        verify(sns).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }

    private void assertFalhaRegistradaSemDetalhe(JsonNode log, String servico, Class<?> classe) {
        assertThat(log.get("level").textValue()).isEqualTo("WARN");
        assertThat(log.get("message").textValue()).isEqualTo("Falha no aquecimento");
        assertThat(log.get("servico").textValue()).isEqualTo(servico);
        assertThat(log.get("classe").textValue()).isEqualTo(classe.getName());
        assertThat(log.has("error")).as("pilha ou mensagem da exceção em %s", log).isFalse();
        assertThat(log.has("alarme")).as("marcador de alarme em %s", log).isFalse();
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR).doesNotContain("FALHA_FUNCAO");
    }

    @Test
    @DisplayName("A Query vai ao índice por-data, numa partição que nunca existe, com limite de 1")
    void queryNaParticaoVazia() {
        aquecimento.beforeCheckpoint(null);

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamo).query(captor.capture());
        QueryRequest consulta = captor.getValue();
        assertThat(consulta.tableName()).isEqualTo(TABELA);
        assertThat(consulta.indexName()).isEqualTo("por-data");
        assertThat(consulta.keyConditionExpression()).isEqualTo("dataEnvio = :dia");
        assertThat(consulta.expressionAttributeValues().get(":dia").s()).isEqualTo("aquecimento");
        assertThat(consulta.limit()).isEqualTo(1);
        verifyNoMoreInteractions(dynamo);
    }

    @Test
    @DisplayName("HeadBucket no bucket de relatórios e GetTopicAttributes no tópico de administradores")
    void leiturasNoBucketENoTopico() {
        aquecimento.beforeCheckpoint(null);

        ArgumentCaptor<HeadBucketRequest> bucket = ArgumentCaptor.forClass(HeadBucketRequest.class);
        verify(s3).headBucket(bucket.capture());
        assertThat(bucket.getValue().bucket()).isEqualTo(BUCKET);
        ArgumentCaptor<GetTopicAttributesRequest> topico = ArgumentCaptor.forClass(GetTopicAttributesRequest.class);
        verify(sns).getTopicAttributes(topico.capture());
        assertThat(topico.getValue().topicArn()).isEqualTo(TOPICO);
    }

    @Test
    @DisplayName("O hook nunca grava objeto nem publica: só uma leitura por cliente")
    void nadaGravadoNemPublicado() {
        aquecimento.beforeCheckpoint(null);

        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(sns, never()).publish(any(PublishRequest.class));
        assertTresClientesChamados();
        verifyNoMoreInteractions(dynamo, s3, sns);
    }

    @Test
    @DisplayName("Sucesso registra em INFO cada cliente aquecido, sem WARN nem ERROR")
    void sucessoRegistraClientesAquecidos() throws Exception {
        aquecimento.beforeCheckpoint(null);

        List<JsonNode> logs = logs();
        assertThat(logs).extracting(log -> log.get("level").textValue()).containsOnly("INFO");
        assertThat(logs).extracting(log -> log.get("message").textValue()).containsOnly("Cliente aquecido");
        assertThat(logs).extracting(log -> log.get("servico").textValue() + " " + log.get("operacao").textValue())
                .containsExactly("DynamoDB Query", "S3 HeadBucket", "SNS GetTopicAttributes");
        assertThat(logs).allSatisfy(log -> assertThat(log.get("duracaoMs").isIntegralNumber()).isTrue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DynamoDB", "S3", "SNS"})
    @DisplayName("A falha de um cliente não propaga e não impede os outros dois")
    void falhaDeUmClienteNaoImpedeOsOutros(String servico) throws Exception {
        falharCliente(servico, SdkClientException.create("Sem rede " + MARCADOR));

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        assertTresClientesChamados();
        assertFalhaRegistradaSemDetalhe(logDoServico(servico), servico, SdkClientException.class);
        assertThat(logDoServico(servico).has("codigoErro")).isFalse();
        assertThat(logs()).filteredOn(log -> "INFO".equals(log.get("level").textValue())).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DynamoDB", "S3", "SNS"})
    @DisplayName("LinkageError de um cliente não propaga e não impede os outros dois")
    void linkageErrorNaoImpedeOsOutros(String servico) throws Exception {
        falharCliente(servico, new NoClassDefFoundError("classe/" + MARCADOR));

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        assertTresClientesChamados();
        assertFalhaRegistradaSemDetalhe(logDoServico(servico), servico, NoClassDefFoundError.class);
    }

    @Test
    @DisplayName("Falha do serviço registra serviço, classe e código, sem a mensagem")
    void falhaDoServico() throws Exception {
        falharCliente("S3", S3Exception.builder()
                .message("Negado " + MARCADOR)
                .statusCode(403)
                .requestId("req-1")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDenied")
                        .errorMessage("Negado " + MARCADOR)
                        .build())
                .build());

        assertThatCode(() -> aquecimento.beforeCheckpoint(null)).doesNotThrowAnyException();

        JsonNode log = logDoServico("S3");
        assertFalhaRegistradaSemDetalhe(log, "S3", S3Exception.class);
        assertThat(log.get("codigoErro").textValue()).isEqualTo("AccessDenied");
        assertThat(log.has("idRequisicao")).isFalse();
    }

    @Test
    @DisplayName("Depois da restauração o hook não chama nenhum cliente")
    void afterRestoreNaoChama() {
        aquecimento.afterRestore(null);

        verifyNoInteractions(dynamo, s3, sns);
        assertThat(appender.linhas).isEmpty();
    }
}
