package br.com.fiap.feedback.relatorio;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.DataReferencia;
import br.com.fiap.feedback.dominio.DataReferenciaInvalidaException;
import br.com.fiap.feedback.relatorio.GeradorRelatorio.Resultado;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.crac.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsBuilder;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Gera o relatório semanal, disparado pelo agendamento ou por invocação direta com
 * {@code dataReferencia} opcional. Converte o evento na data de referência, chama
 * {@link GeradorRelatorio} e converte o resultado na resposta.
 *
 * <p>Toda falha é registrada e relançada, para a invocação terminar com {@code FunctionError}. A
 * exceção relançada tem mensagem fixa, e exceção do SDK é registrada só pela classe, código de erro e
 * ID da requisição. Descrição, nota individual e média não vão para log, em nível nenhum.
 *
 * <p>As linhas que representam execução com falha levam o campo {@code alarme} com o valor
 * {@code FALHA_FUNCAO}, que o filtro de métrica do alarme de falha procura: a falha na leitura,
 * na gravação ou na publicação e o {@code Error} que escapa da invocação. A data de referência
 * rejeitada não leva o marcador, porque não é falha da função.
 */
public class GerarRelatorioSemanalHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // Precisa vir antes do logger: a JVM guarda o valor na primeira resolução de nome.
    static {
        Security.setProperty("networkaddress.cache.ttl", "0");
        TracingUtils.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GerarRelatorioSemanalHandler.class);

    private static final String CAMPO_DATA_REFERENCIA = "dataReferencia";
    private static final String METRICA_GERADO = "RelatorioGerado";
    private static final String METRICA_REJEITADA = "DataReferenciaRejeitada";
    private static final String METRICA_ERRO_INTERNO = "ErroInterno";
    private static final String DESCONHECIDO = "desconhecido";
    private static final String CAMPO_ALARME = "alarme";
    private static final String MARCADOR_ALARME = "FALHA_FUNCAO";

    // Sem paginação extra são 9 chamadas, que somam 90 s no pior caso, abaixo do Timeout de 120 s da
    // função. O SDK precisa desistir antes, senão o Lambda encerra a invocação sem passar pelo catch.
    private static final Duration TEMPO_MAXIMO_CHAMADA = Duration.ofSeconds(10);

    /**
     * Recursos de produção. A classe só é carregada pelo construtor público, que o runtime chama na
     * inicialização, então eles entram no snapshot do SnapStart. Os testes usam o outro construtor e
     * não dependem de região, credenciais nem rede. Criar os clientes não chama a AWS.
     *
     * <p>Os clientes ficam em campos próprios porque o gerador e o aquecimento usam as mesmas
     * instâncias. O contexto do CRaC guarda só referência fraca, por isso o aquecimento também fica
     * num campo estático.
     */
    private static final class Recursos {
        static final DynamoDbClient DYNAMO = criarCliente(DynamoDbClient.builder());
        static final S3Client S3 = criarCliente(S3Client.builder());
        static final SnsClient SNS = criarCliente(SnsClient.builder());
        static final String TABELA = variavel("TABELA_AVALIACOES");
        static final String BUCKET = variavel("BUCKET_RELATORIOS");
        static final String TOPICO = variavel("TOPICO_ADMINS_ARN");
        static final GeradorRelatorio GERADOR = new GeradorRelatorio(
                new LeitorAvaliacoes(DYNAMO, TABELA),
                S3,
                SNS,
                variavel("AMBIENTE"),
                BUCKET,
                TOPICO,
                Rastreador.XRAY);
        static final Metrics METRICAS = MetricsBuilder.builder().build();
        static final Aquecimento AQUECIMENTO = new Aquecimento(DYNAMO, TABELA, S3, BUCKET, SNS, TOPICO);

        static {
            Core.getGlobalContext().register(AQUECIMENTO);
        }

        private static String variavel(String nome) {
            return Objects.requireNonNull(System.getenv(nome), "Variável " + nome + " não definida.");
        }
    }

    private final GeradorRelatorio gerador;
    private final Clock relogio;
    private final Metrics metricas;

    /** Construtor usado pelo runtime do Lambda. */
    public GerarRelatorioSemanalHandler() {
        this(Recursos.GERADOR, Clock.systemUTC(), Recursos.METRICAS);
    }

    GerarRelatorioSemanalHandler(GeradorRelatorio gerador, Clock relogio, Metrics metricas) {
        this.gerador = gerador;
        this.relogio = relogio;
        this.metricas = metricas;
    }

    static <B extends SdkSyncClientBuilder<B, C> & SdkClientBuilder<B, C>, C> C criarCliente(B construtor) {
        return construtor
                .httpClient(UrlConnectionHttpClient.create())
                .overrideConfiguration(configuracao -> configuracao.apiCallTimeout(TEMPO_MAXIMO_CHAMADA))
                .build();
    }

    /**
     * Trata uma invocação.
     *
     * @param evento objeto com {@code dataReferencia} opcional; sem ele, a referência é o dia atual em
     *     America/Sao_Paulo
     * @param contexto contexto da invocação
     * @return data de referência, chave do objeto gravado e quantidade de avaliações
     * @throws DataReferenciaInvalidaException se a data informada for inválida, sem nenhum efeito
     * @throws RuntimeException se a leitura, a gravação ou a publicação falhar
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> evento, Context contexto) {
        try {
            // Antes do withLogging, que marca o fim do cold start no estado global do Powertools.
            emitirMetrica(() -> metricas.captureColdStartMetric(contexto));
            return PowertoolsLogging.withLogging(contexto, () -> {
                try {
                    return processar(evento);
                } finally {
                    emitirMetrica(metricas::flush);
                }
            });
        } catch (Error e) {
            registrarErro(e);
            throw e;
        }
    }

    // Um Error, como NoClassDefFoundError ou OutOfMemoryError, passa pelos catch de RuntimeException
    // e terminaria a invocação sem marcador. Só a classe vai para o log; quem chama recebe o mesmo Error.
    private static void registrarErro(Error e) {
        LOG.error("Erro na execução da função",
                entry("classe", e.getClass().getName()),
                entry(CAMPO_ALARME, MARCADOR_ALARME));
    }

    private Map<String, Object> processar(Map<String, Object> evento) {
        try {
            // Calculada aqui, nunca na inicialização: com SnapStart, repetiria a data do snapshot.
            DataReferencia referencia = DataReferencia.de(dataInformada(evento), relogio);
            Resultado resultado = gerador.gerar(referencia);
            contar(METRICA_GERADO);
            return Map.of(
                    CAMPO_DATA_REFERENCIA, referencia.toString(),
                    "chave", resultado.localizacao().chave(),
                    "quantidadeAvaliacoes", resultado.resumo().quantidadeTotal());
        } catch (DataReferenciaInvalidaException e) {
            LOG.error("Data de referência rejeitada", entry("classe", e.getClass().getName()));
            contar(METRICA_REJEITADA);
            throw e;
        } catch (RuntimeException e) {
            registrarFalha(e);
            contar(METRICA_ERRO_INTERNO);
            throw new GeracaoRelatorioException();
        }
    }

    private static Object dataInformada(Map<String, Object> evento) {
        return evento == null ? null : evento.get(CAMPO_DATA_REFERENCIA);
    }

    // Nunca passa a exceção ao logger: mensagem e pilha podem citar dados lidos.
    private static void registrarFalha(RuntimeException e) {
        String codigoErro = DESCONHECIDO;
        String idRequisicao = DESCONHECIDO;
        if (e instanceof AwsServiceException servico) {
            AwsErrorDetails detalhes = servico.awsErrorDetails();
            if (detalhes != null && detalhes.errorCode() != null) {
                codigoErro = detalhes.errorCode();
            }
            if (servico.requestId() != null) {
                idRequisicao = servico.requestId();
            }
        }
        LOG.error("Falha ao gerar relatório",
                entry("classe", e.getClass().getName()),
                entry("codigoErro", codigoErro),
                entry("idRequisicao", idRequisicao),
                entry(CAMPO_ALARME, MARCADOR_ALARME));
    }

    private void contar(String metrica) {
        emitirMetrica(() -> metricas.addMetric(metrica, 1, MetricUnit.COUNT));
    }

    // Falha de métrica não pode trocar o resultado da execução.
    private void emitirMetrica(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            LOG.warn("Falha ao emitir métrica", entry("classe", e.getClass().getName()));
        }
    }
}
