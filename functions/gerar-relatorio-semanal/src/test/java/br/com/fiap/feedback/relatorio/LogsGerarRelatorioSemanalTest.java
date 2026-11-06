package br.com.fiap.feedback.relatorio;

import static br.com.fiap.feedback.relatorio.ApoioTeste.BUCKET;
import static br.com.fiap.feedback.relatorio.ApoioTeste.SEGUNDA_08H;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TABELA;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TOPICO;
import static br.com.fiap.feedback.relatorio.ApoioTeste.contexto;
import static br.com.fiap.feedback.relatorio.ApoioTeste.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.dominio.DataReferenciaInvalidaException;
import br.com.fiap.feedback.relatorio.ApoioTeste.Cenario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * Captura os logs com o layout real do {@code log4j2.xml} e o nível mais baixo possível, para que a
 * ausência do dado valha em todos os níveis, inclusive nos campos de exceção do layout.
 */
class LogsGerarRelatorioSemanalTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MARCADOR = "TEXTO-MARCADOR-LOG";
    // O filtro de métrica busca por substring, por isso a ausência é conferida no texto da linha.
    private static final String MARCADOR_ALARME = "FALHA_FUNCAO";

    private AppenderEmMemoria appender;
    private Level nivelOriginal;
    private Cenario cenario;

    @BeforeEach
    void preparar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        Configuration configuracao = contextoLog.getConfiguration();
        nivelOriginal = configuracao.getRootLogger().getLevel();

        appender = new AppenderEmMemoria(configuracao.getAppender("SaidaJson").getLayout());
        appender.start();
        configuracao.getRootLogger().addAppender(appender, Level.ALL, null);
        Configurator.setRootLevel(Level.ALL);

        cenario = new Cenario();
    }

    @AfterEach
    void limpar() {
        LoggerContext contextoLog = (LoggerContext) LogManager.getContext(false);
        contextoLog.getConfiguration().getRootLogger().removeAppender(appender.getName());
        Configurator.setRootLevel(nivelOriginal);
        appender.stop();
    }

    private static void comAvaliacoes(Cenario alvo, int notaA, int notaB) {
        alvo.dynamo.adicionar(item("id-a", MARCADOR, notaA, "MEDIA", "2026-11-03"));
        alvo.dynamo.adicionar(item("id-b", MARCADOR, notaB, "MEDIA", "2026-11-05"));
    }

    private List<JsonNode> logsEmJson() throws IOException {
        List<JsonNode> logs = new ArrayList<>();
        for (String linha : appender.linhas) {
            logs.add(JSON.readTree(linha));
        }
        return logs;
    }

    private JsonNode logDeNivel(String nivel) throws IOException {
        return logsEmJson().stream()
                .filter(log -> nivel.equals(log.get("level").textValue()))
                .findFirst()
                .orElseThrow();
    }

    private List<JsonNode> linhasComMarcadorDeAlarme() throws IOException {
        return logsEmJson().stream().filter(log -> log.toString().contains(MARCADOR_ALARME)).toList();
    }

    private void assertSemMarcadorDeAlarme() {
        assertThat(appender.linhas).isNotEmpty();
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR_ALARME);
    }

    private static void assertComMarcadorDeAlarme(JsonNode log) {
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("alarme").textValue()).isEqualTo(MARCADOR_ALARME);
    }

    /** Campos que variam entre execuções iguais, fora do que o handler registra. */
    private static JsonNode semCamposVariaveis(JsonNode log) {
        ObjectNode copia = log.deepCopy();
        copia.remove(List.of("timestamp", "cold_start", "function_request_id"));
        return copia;
    }

    private void assertSemDadoDoAluno() throws IOException {
        assertThat(appender.linhas).isNotEmpty();
        assertThat(String.join("\n", appender.linhas)).doesNotContain(MARCADOR);
        for (JsonNode log : logsEmJson()) {
            assertThat(log.has("nota")).as("atributo nota em %s", log).isFalse();
            assertThat(log.has("descricao")).as("atributo descricao em %s", log).isFalse();
            assertThat(log.has("media")).as("atributo media em %s", log).isFalse();
            assertThat(log.has("error")).as("atributo error em %s", log).isFalse();
        }
    }

    /**
     * Roda o mesmo cenário com notas diferentes da mesma urgência. Se os logs saem idênticos, nenhuma
     * nota e nenhuma média foram registradas: buscar um dígito solto não provaria isso, porque as
     * datas e os totais também têm dígitos.
     */
    private List<JsonNode> logsComNotas(int notaA, int notaB, Consumer<Cenario> preparo) throws IOException {
        appender.linhas.clear();
        Cenario alvo = new Cenario();
        comAvaliacoes(alvo, notaA, notaB);
        preparo.accept(alvo);
        try {
            alvo.executarNaSegunda();
        } catch (GeracaoRelatorioException e) {
            assertThat(e.getMessage()).doesNotContain(MARCADOR);
        }
        return logsEmJson().stream().map(LogsGerarRelatorioSemanalTest::semCamposVariaveis).toList();
    }

    private void assertLogsNaoDependemDasNotas(Consumer<Cenario> preparo) throws IOException {
        // Médias 4,50 e 5,50, e as duas notas mudam: o log de qualquer uma delas ou da média apareceria aqui.
        List<JsonNode> comNotas4E5 = logsComNotas(4, 5, preparo);
        List<JsonNode> comNotas5E6 = logsComNotas(5, 6, preparo);

        assertThat(comNotas4E5).isNotEmpty().isEqualTo(comNotas5E6);
    }

    @Test
    @DisplayName("Geração com avaliações: sem marcador e sem atributo de nota, em nenhum nível")
    void geracaoComAvaliacoes() throws Exception {
        comAvaliacoes(cenario, 4, 6);

        cenario.executarNaSegunda();

        assertThat(cenario.s3.objeto("relatorios/2026-11-09.txt").texto()).contains(MARCADOR);
        assertSemDadoDoAluno();
    }

    @Test
    @DisplayName("Sucesso: os logs não mudam quando só as notas mudam")
    void sucessoNaoDependeDasNotas() throws Exception {
        assertLogsNaoDependemDasNotas(alvo -> {
        });
    }

    @Test
    @DisplayName("Falha na gravação: os logs não mudam quando só as notas mudam")
    void falhaNaGravacaoNaoDependeDasNotas() throws Exception {
        assertLogsNaoDependemDasNotas(
                alvo -> alvo.s3.falha = S3Exception.builder().message("negado").statusCode(403).build());
    }

    @Test
    @DisplayName("Falha na publicação: os logs não mudam quando só as notas mudam")
    void falhaNaPublicacaoNaoDependeDasNotas() throws Exception {
        assertLogsNaoDependemDasNotas(
                alvo -> alvo.sns.falha = SnsException.builder().message("negado").statusCode(403).build());
    }

    @Test
    @DisplayName("Sucesso registra data de referência, quantidade total, totais por urgência, bucket e chave")
    void sucessoRegistraOPermitido() throws Exception {
        comAvaliacoes(cenario, 4, 6);

        cenario.executarNaSegunda();

        JsonNode log = logsEmJson().getLast();
        assertThat(log.get("level").textValue()).isEqualTo("INFO");
        assertThat(log.get("message").textValue()).isEqualTo("Relatório gerado");
        assertThat(log.get("dataReferencia").textValue()).isEqualTo("2026-11-09");
        assertThat(log.get("quantidadeTotal").intValue()).isEqualTo(2);
        assertThat(log.get("totaisPorUrgencia").get("MEDIA").intValue()).isEqualTo(2);
        assertThat(log.get("totaisPorUrgencia").get("CRITICA").intValue()).isZero();
        assertThat(log.get("bucket").textValue()).isEqualTo(ApoioTeste.BUCKET);
        assertThat(log.get("chave").textValue()).isEqualTo("relatorios/2026-11-09.txt");
        assertThat(log.get("function_name").textValue()).isEqualTo("funcao-teste");
    }

    @Test
    @DisplayName("Falha na publicação deixa no log que o relatório foi gravado")
    void falhaNaPublicacaoMostraAGravacao() throws Exception {
        cenario.sns.falha = SnsException.builder().message("negado").statusCode(403).build();

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertThat(logsEmJson())
                .extracting(log -> log.get("message").textValue())
                .containsExactly("Relatório gravado", "Falha ao gerar relatório");
    }

    @Test
    @DisplayName("Falha do serviço registra classe, código e ID da requisição, sem a mensagem")
    void falhaDoServico() throws Exception {
        comAvaliacoes(cenario, 4, 6);
        cenario.dynamo.falharEm(
                "2026-11-05",
                0,
                DynamoDbException.builder()
                        .message("Falhou com " + MARCADOR)
                        .requestId("req-1")
                        .statusCode(400)
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorCode("ValidationException")
                                .errorMessage("Falhou com " + MARCADOR)
                                .build())
                        .build());

        assertThatThrownBy(cenario::executarNaSegunda)
                .isInstanceOf(GeracaoRelatorioException.class)
                .hasNoCause()
                .hasMessageNotContaining(MARCADOR);

        assertSemDadoDoAluno();
        JsonNode log = logDeNivel("ERROR");
        assertThat(log.get("classe").textValue()).isEqualTo(DynamoDbException.class.getName());
        assertThat(log.get("codigoErro").textValue()).isEqualTo("ValidationException");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("Falha sem resposta do serviço registra só a classe")
    void falhaDoCliente() throws Exception {
        cenario.s3.falha = SdkClientException.create("Falhou com " + MARCADOR);

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertSemDadoDoAluno();
        JsonNode log = logDeNivel("ERROR");
        assertThat(log.get("classe").textValue()).isEqualTo(SdkClientException.class.getName());
        assertThat(log.get("codigoErro").textValue()).isEqualTo("desconhecido");
        assertThat(log.get("idRequisicao").textValue()).isEqualTo("desconhecido");
    }

    @Test
    @DisplayName("Item inválido registra id e nomes dos campos, sem os valores")
    void itemInvalido() throws Exception {
        var invalido = item("id-invalido", MARCADOR, 5, "VALOR-DESCONHECIDO", "2026-11-05");
        invalido.remove("nota");
        cenario.dynamo.adicionar(invalido);

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertSemDadoDoAluno();
        assertThat(String.join("\n", appender.linhas)).doesNotContain("VALOR-DESCONHECIDO");
        JsonNode log = logsEmJson().getFirst();
        assertThat(log.get("level").textValue()).isEqualTo("ERROR");
        assertThat(log.get("id").textValue()).isEqualTo("id-invalido");
        assertThat(log.get("campos").toString()).contains("nota", "urgencia");
    }

    @Test
    @DisplayName("Item inválido sem id não quebra o log")
    void itemInvalidoSemId() throws Exception {
        var invalido = item("x", MARCADOR, 5, "MEDIA", "2026-11-05");
        invalido.remove("id");
        cenario.dynamo.adicionar(invalido);

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertThat(logsEmJson().getFirst().get("id").textValue()).isEqualTo("desconhecido");
    }

    @Test
    @DisplayName("Data rejeitada não registra o valor recebido")
    void dataRejeitada() throws Exception {
        assertThatThrownBy(() -> cenario.executarPara(MARCADOR))
                .isInstanceOf(DataReferenciaInvalidaException.class)
                .hasMessageNotContaining(MARCADOR);

        assertSemDadoDoAluno();
        assertThat(logDeNivel("ERROR").get("message").textValue()).isEqualTo("Data de referência rejeitada");
    }

    @ParameterizedTest
    @ValueSource(strings = {"leitura", "gravação", "publicação"})
    @DisplayName("Execução com falha grava uma única linha com o marcador de alarme, a da falha")
    void falhaComMarcador(String etapa) throws Exception {
        comAvaliacoes(cenario, 4, 6);
        switch (etapa) {
            case "leitura" -> cenario.dynamo.falharEm(
                    "2026-11-05", 0, DynamoDbException.builder().message("falha").statusCode(500).build());
            case "gravação" -> cenario.s3.falha = S3Exception.builder().message("negado").statusCode(403).build();
            case "publicação" -> cenario.sns.falha = SnsException.builder().message("negado").statusCode(403).build();
            default -> throw new IllegalArgumentException(etapa);
        }

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertSemDadoDoAluno();
        List<JsonNode> marcadas = linhasComMarcadorDeAlarme();
        assertThat(marcadas).hasSize(1);
        assertComMarcadorDeAlarme(marcadas.getFirst());
        assertThat(marcadas.getFirst().get("message").textValue()).isEqualTo("Falha ao gerar relatório");
    }

    @Test
    @DisplayName("Item inválido: só a linha da execução com falha leva o marcador")
    void itemInvalidoMarcaSoAExecucao() throws Exception {
        var invalido = item("id-invalido", MARCADOR, 5, "MEDIA", "2026-11-05");
        invalido.remove("nota");
        cenario.dynamo.adicionar(invalido);

        assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

        assertThat(logsEmJson()).extracting(log -> log.get("message").textValue())
                .containsExactly("Item de avaliação inválido", "Falha ao gerar relatório");
        assertThat(linhasComMarcadorDeAlarme())
                .extracting(log -> log.get("message").textValue())
                .containsExactly("Falha ao gerar relatório");
    }

    @Test
    @DisplayName("Data de referência 2026-02-30 é rejeitada sem o marcador de alarme")
    void dataRejeitadaSemMarcador() throws Exception {
        assertThatThrownBy(() -> cenario.executarPara("2026-02-30"))
                .isInstanceOf(DataReferenciaInvalidaException.class);

        assertSemMarcadorDeAlarme();
        assertThat(logDeNivel("ERROR").get("message").textValue()).isEqualTo("Data de referência rejeitada");
        assertThat(logDeNivel("ERROR").has("alarme")).isFalse();
    }

    @Test
    @DisplayName("Relatório gerado e avisado com sucesso não grava linha em WARN ou ERROR nem o marcador")
    void sucessoSemWarnNemMarcador() throws Exception {
        comAvaliacoes(cenario, 4, 6);

        cenario.executarNaSegunda();

        assertThat(cenario.sns.publicacoes).hasSize(1);
        assertThat(logsEmJson())
                .extracting(log -> log.get("level").textValue())
                .isNotEmpty()
                .doesNotContain("WARN", "ERROR");
        assertSemMarcadorDeAlarme();
    }

    @Test
    @DisplayName("Error que escapa da invocação é registrado só pela classe, com o marcador, e relançado")
    void errorEscapadoComMarcador() throws Exception {
        comAvaliacoes(cenario, 4, 6);
        NoClassDefFoundError erro = new NoClassDefFoundError("classe/" + MARCADOR);
        S3Client s3 = mock(S3Client.class);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(erro);
        GeradorRelatorio gerador = new GeradorRelatorio(
                new LeitorAvaliacoes(cenario.dynamo, TABELA), s3, cenario.sns, "dev", BUCKET, TOPICO, cenario.rastreador);
        GerarRelatorioSemanalHandler handler = new GerarRelatorioSemanalHandler(
                gerador, Clock.fixed(SEGUNDA_08H, ZoneOffset.UTC), cenario.metricas);

        assertThatThrownBy(() -> handler.handleRequest(Map.of(), contexto())).isSameAs(erro);

        assertSemDadoDoAluno();
        List<JsonNode> logs = logsEmJson();
        assertThat(logs).hasSize(1);
        JsonNode log = logs.getFirst();
        assertComMarcadorDeAlarme(log);
        assertThat(log.get("message").textValue()).isEqualTo("Erro na execução da função");
        assertThat(log.get("classe").textValue()).isEqualTo(NoClassDefFoundError.class.getName());
        assertThat(cenario.sns.publicacoes).isEmpty();
        verify(cenario.metricas).flush();
    }

    // Prova de que a captura enxerga todos os níveis e o texto de exceções. Sem isso, as verificações
    // de ausência acima passariam mesmo com o appender quebrado.
    @Test
    @DisplayName("A captura enxerga todos os níveis e o texto de exceções")
    void capturaFunciona() {
        Logger log = LoggerFactory.getLogger(GerarRelatorioSemanalHandler.class);

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
