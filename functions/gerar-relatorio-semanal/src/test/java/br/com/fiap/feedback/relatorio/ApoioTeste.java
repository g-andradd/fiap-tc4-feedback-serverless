package br.com.fiap.feedback.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.lambda.powertools.metrics.Metrics;

/** Clientes, contexto e rastreador falsos, sem contato com a AWS. */
final class ApoioTeste {

    static final String TABELA = "tabela-teste";
    static final String BUCKET = "feedback-relatorios-123456789012-dev";
    static final String TOPICO = "arn:aws:sns:sa-east-1:123456789012:feedback-admins-dev";

    /** 08:00 de 09/11/2026, segunda-feira, em São Paulo. */
    static final Instant SEGUNDA_08H = Instant.parse("2026-11-09T11:00:00Z");

    private ApoioTeste() {
    }

    /**
     * Índice {@code por-data} falso: devolve só a partição pedida, página a página. Usa o
     * {@code queryPaginator} real da interface, então a continuação é a do SDK.
     */
    static final class DynamoFalso implements DynamoDbClient {
        private static final String PAGINA = "pagina";

        final List<QueryRequest> consultas = new ArrayList<>();
        final Map<String, List<List<Map<String, AttributeValue>>>> paginasPorDia = new HashMap<>();
        final Map<String, Integer> paginaQueFalha = new HashMap<>();
        RuntimeException falha;

        /** Acrescenta o item à última página da partição de {@code dataEnvio}. */
        void adicionar(Map<String, AttributeValue> item) {
            List<List<Map<String, AttributeValue>>> paginas =
                    paginasPorDia.computeIfAbsent(item.get("dataEnvio").s(), dia -> new ArrayList<>());
            if (paginas.isEmpty()) {
                paginas.add(new ArrayList<>());
            }
            paginas.getLast().add(item);
        }

        /** Define as páginas da partição, cada uma com os seus itens. */
        @SafeVarargs
        final void definirPaginas(String dia, List<Map<String, AttributeValue>>... paginas) {
            paginasPorDia.put(dia, List.of(paginas));
        }

        void falharEm(String dia, int pagina, RuntimeException erro) {
            paginaQueFalha.put(dia, pagina);
            falha = erro;
        }

        @Override
        public QueryResponse query(QueryRequest consulta) {
            consultas.add(consulta);
            String dia = consulta.expressionAttributeValues().get(":dia").s();
            int pagina = consulta.hasExclusiveStartKey()
                    ? Integer.parseInt(consulta.exclusiveStartKey().get(PAGINA).n())
                    : 0;
            Integer falhaNaPagina = paginaQueFalha.get(dia);
            if (falhaNaPagina != null && falhaNaPagina == pagina) {
                throw falha;
            }
            List<List<Map<String, AttributeValue>>> paginas = paginasPorDia.getOrDefault(dia, List.of(List.of()));
            QueryResponse.Builder resposta = QueryResponse.builder().items(paginas.get(pagina));
            if (pagina + 1 < paginas.size()) {
                resposta.lastEvaluatedKey(Map.of(PAGINA, AttributeValue.fromN(Integer.toString(pagina + 1))));
            }
            return resposta.build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }

    /** Objeto gravado no bucket falso. */
    record ObjetoGravado(PutObjectRequest requisicao, byte[] bytes) {
        String texto() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /** Bucket falso: como no S3, gravar na mesma chave substitui o objeto. */
    static final class S3Falso implements S3Client {
        final Map<String, ObjetoGravado> objetos = new LinkedHashMap<>();
        int gravacoes;
        RuntimeException falha;

        ObjetoGravado objeto(String chave) {
            return objetos.get(BUCKET + "/" + chave);
        }

        @Override
        public PutObjectResponse putObject(PutObjectRequest requisicao, RequestBody corpo) {
            gravacoes++;
            if (falha != null) {
                throw falha;
            }
            try (InputStream entrada = corpo.contentStreamProvider().newStream()) {
                objetos.put(requisicao.bucket() + "/" + requisicao.key(), new ObjetoGravado(requisicao, entrada.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }

    /** Tópico falso: guarda as publicações e quantos objetos existiam quando cada uma saiu. */
    static final class SnsFalso implements SnsClient {
        final List<PublishRequest> publicacoes = new ArrayList<>();
        final List<Integer> gravacoesAntesDePublicar = new ArrayList<>();
        S3Falso s3;
        RuntimeException falha;

        @Override
        public PublishResponse publish(PublishRequest requisicao) {
            gravacoesAntesDePublicar.add(s3.gravacoes);
            if (falha != null) {
                throw falha;
            }
            publicacoes.add(requisicao);
            return PublishResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }

    /** Executa a ação sem carregar o recorder do X-Ray e guarda o que foi anotado. */
    static final class RastreadorFalso implements Rastreador {
        final Map<String, String> anotacoes = new HashMap<>();
        final List<String> subsegmentos = new ArrayList<>();

        @Override
        public void emSubsegmento(String nome, Runnable acao) {
            subsegmentos.add(nome);
            acao.run();
        }

        @Override
        public void anotar(String chave, String valor) {
            anotacoes.put(chave, valor);
        }
    }

    /** Liga os falsos ao handler, com o relógio informado. */
    static final class Cenario {
        final DynamoFalso dynamo = new DynamoFalso();
        final S3Falso s3 = new S3Falso();
        final SnsFalso sns = new SnsFalso();
        final RastreadorFalso rastreador = new RastreadorFalso();
        final Metrics metricas = mock(Metrics.class);

        Cenario() {
            sns.s3 = s3;
        }

        GerarRelatorioSemanalHandler handler(Instant instante) {
            GeradorRelatorio gerador = new GeradorRelatorio(
                    new LeitorAvaliacoes(dynamo, TABELA), s3, sns, "dev", BUCKET, TOPICO, rastreador);
            return new GerarRelatorioSemanalHandler(gerador, Clock.fixed(instante, ZoneOffset.UTC), metricas);
        }

        Map<String, Object> executar(Instant instante, Map<String, Object> evento) {
            return handler(instante).handleRequest(evento, contexto());
        }

        /** Execução agendada: o evento é {@code {}} e a referência é o dia da segunda-feira. */
        Map<String, Object> executarNaSegunda() {
            return executar(SEGUNDA_08H, Map.of());
        }

        /** Acionamento manual com data de referência. */
        Map<String, Object> executarPara(String dataReferencia) {
            return executar(SEGUNDA_08H, Map.of("dataReferencia", dataReferencia));
        }

        void semNenhumEfeito() {
            assertThat(s3.gravacoes).isZero();
            assertThat(sns.gravacoesAntesDePublicar).isEmpty();
        }
    }

    /** Item como o registro grava, com os seis atributos. */
    static Map<String, AttributeValue> item(String id, String descricao, int nota, String urgencia, String dataEnvio) {
        return item(id, descricao, nota, urgencia, dataEnvio, dataEnvio + "T12:00:00.000Z");
    }

    static Map<String, AttributeValue> item(
            String id, String descricao, int nota, String urgencia, String dataEnvio, String criadoEm) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("descricao", AttributeValue.fromS(descricao));
        item.put("nota", AttributeValue.fromN(Integer.toString(nota)));
        item.put("urgencia", AttributeValue.fromS(urgencia));
        item.put("dataEnvio", AttributeValue.fromS(dataEnvio));
        item.put("criadoEm", AttributeValue.fromS(criadoEm));
        return item;
    }

    static Context contexto() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "requisicao-teste";
            }

            @Override
            public String getLogGroupName() {
                return "grupo-teste";
            }

            @Override
            public String getLogStreamName() {
                return "fluxo-teste";
            }

            @Override
            public String getFunctionName() {
                return "funcao-teste";
            }

            @Override
            public String getFunctionVersion() {
                return "1";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "arn-teste";
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 120_000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 512;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };
    }
}
