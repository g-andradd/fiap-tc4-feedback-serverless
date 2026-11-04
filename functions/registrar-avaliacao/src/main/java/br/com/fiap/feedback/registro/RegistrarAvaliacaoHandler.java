package br.com.fiap.feedback.registro;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.AtributosAvaliacao;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.ValidadorAvaliacao;
import br.com.fiap.feedback.dominio.ValorEntrada;
import br.com.fiap.feedback.registro.LeitorCorpo.EntradaAvaliacao;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsBuilder;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Atende {@code POST /avaliacao}: converte o corpo, valida, grava a avaliação na tabela e devolve
 * identificador e urgência.
 *
 * <p>Descrição, nota e corpo bruto não vão para log nem para trace, em nível nenhum. Exceção do
 * Jackson é registrada só pela classe, e a do SDK pela classe, código de erro e ID da requisição.
 */
public class RegistrarAvaliacaoHandler
        implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    // Precisa vir antes do logger: a JVM guarda o valor na primeira resolução de nome.
    static {
        Security.setProperty("networkaddress.cache.ttl", "0");
        TracingUtils.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarAvaliacaoHandler.class);

    private static final String METRICA_REGISTRADA = "AvaliacaoRegistrada";
    private static final String METRICA_REJEITADA = "EntradaRejeitada";
    private static final String METRICA_ERRO_INTERNO = "ErroInterno";
    private static final String DESCONHECIDO = "desconhecido";

    // Abaixo do Timeout de 10 s da função: o SDK precisa desistir antes, senão o Lambda encerra a
    // invocação sem passar pelo catch e o cliente não recebe o 500 genérico.
    private static final Duration TEMPO_MAXIMO_CHAMADA = Duration.ofSeconds(8);

    /**
     * Recursos de produção. A classe só é carregada pelo construtor público, que o runtime chama na
     * inicialização, então eles entram no snapshot do SnapStart. Os testes usam o outro construtor e
     * não dependem de região, credenciais nem rede. Criar o cliente não chama a AWS.
     */
    private static final class Recursos {
        static final DynamoDbClient CLIENTE = criarCliente(DynamoDbClient.builder());
        static final String TABELA = Objects.requireNonNull(
                System.getenv("TABELA_AVALIACOES"), "Variável TABELA_AVALIACOES não definida.");
        static final Metrics METRICAS = MetricsBuilder.builder().build();
    }

    private final DynamoDbClient cliente;
    private final String tabela;
    private final Clock relogio;
    private final Metrics metricas;
    private final Rastreador rastreador;

    /** Construtor usado pelo runtime do Lambda. */
    public RegistrarAvaliacaoHandler() {
        this(Recursos.CLIENTE, Recursos.TABELA, Clock.systemUTC(), Recursos.METRICAS, Rastreador.XRAY);
    }

    RegistrarAvaliacaoHandler(
            DynamoDbClient cliente, String tabela, Clock relogio, Metrics metricas, Rastreador rastreador) {
        this.cliente = cliente;
        this.tabela = tabela;
        this.relogio = relogio;
        this.metricas = metricas;
        this.rastreador = rastreador;
    }

    static DynamoDbClient criarCliente(DynamoDbClientBuilder construtor) {
        return construtor
                .httpClient(UrlConnectionHttpClient.create())
                .overrideConfiguration(configuracao -> configuracao.apiCallTimeout(TEMPO_MAXIMO_CHAMADA))
                .build();
    }

    /**
     * Trata uma requisição.
     *
     * @param evento evento do API Gateway, com payload 2.0
     * @param contexto contexto da invocação
     * @return 201 no sucesso, 400 para entrada rejeitada e 500 para falha interna
     */
    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent evento, Context contexto) {
        // Antes do withLogging, que marca o fim do cold start no estado global do Powertools.
        emitirMetrica(() -> metricas.captureColdStartMetric(contexto));
        return PowertoolsLogging.withLogging(contexto, () -> {
            try {
                return processar(evento);
            } finally {
                emitirMetrica(metricas::flush);
            }
        });
    }

    private APIGatewayV2HTTPResponse processar(APIGatewayV2HTTPEvent evento) {
        try {
            EntradaAvaliacao entrada = LeitorCorpo.ler(evento.getBody(), evento.getIsBase64Encoded());
            List<String> invalidos = ValidadorAvaliacao.validar(entrada.descricao(), entrada.nota());
            if (!invalidos.isEmpty()) {
                return rejeitarCampos(invalidos);
            }
            return registrar(entrada);
        } catch (CorpoInvalidoException e) {
            return rejeitarCorpo(e);
        } catch (RuntimeException e) {
            return falhar(e);
        }
    }

    private APIGatewayV2HTTPResponse registrar(EntradaAvaliacao entrada) {
        // Gerados aqui, nunca em campo estático: com SnapStart, repetiriam entre ambientes restaurados.
        Avaliacao avaliacao = Avaliacao.nova(
                ((ValorEntrada.Texto) entrada.descricao()).valor(),
                ((ValorEntrada.Inteiro) entrada.nota()).valor().intValueExact(),
                UUID.randomUUID().toString(),
                Instant.now(relogio));

        rastreador.emSubsegmento("gravarAvaliacao", () -> {
            rastreador.anotar(AtributosAvaliacao.URGENCIA, avaliacao.urgencia().name());
            cliente.putItem(PutItemRequest.builder().tableName(tabela).item(paraItem(avaliacao)).build());
        });

        LOG.info("Avaliação registrada",
                entry(AtributosAvaliacao.ID, avaliacao.id()),
                entry(AtributosAvaliacao.URGENCIA, avaliacao.urgencia().name()));
        contar(METRICA_REGISTRADA);
        return Respostas.sucesso(avaliacao);
    }

    private APIGatewayV2HTTPResponse rejeitarCampos(List<String> invalidos) {
        LOG.info("Entrada rejeitada", entry("campos", invalidos));
        contar(METRICA_REJEITADA);
        return Respostas.camposInvalidos(invalidos);
    }

    private APIGatewayV2HTTPResponse rejeitarCorpo(CorpoInvalidoException e) {
        LOG.info("Corpo rejeitado", entry("motivo", e.getMessage()));
        contar(METRICA_REJEITADA);
        return Respostas.corpoInvalido();
    }

    // Nunca passa a exceção ao logger: mensagem e pilha podem citar dados de entrada.
    private APIGatewayV2HTTPResponse falhar(RuntimeException e) {
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
        LOG.error("Falha ao registrar avaliação",
                entry("classe", e.getClass().getName()),
                entry("codigoErro", codigoErro),
                entry("idRequisicao", idRequisicao));
        contar(METRICA_ERRO_INTERNO);
        return Respostas.erroInterno();
    }

    private void contar(String metrica) {
        emitirMetrica(() -> metricas.addMetric(metrica, 1, MetricUnit.COUNT));
    }

    // Falha de métrica não pode trocar a resposta: a avaliação pode já estar gravada, e um 500 levaria
    // o cliente a reenviar e duplicar o registro.
    private void emitirMetrica(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            LOG.warn("Falha ao emitir métrica", entry("classe", e.getClass().getName()));
        }
    }

    private static Map<String, AttributeValue> paraItem(Avaliacao avaliacao) {
        return Map.of(
                AtributosAvaliacao.ID, AttributeValue.fromS(avaliacao.id()),
                AtributosAvaliacao.DESCRICAO, AttributeValue.fromS(avaliacao.descricao()),
                AtributosAvaliacao.NOTA, AttributeValue.fromN(Integer.toString(avaliacao.nota())),
                AtributosAvaliacao.URGENCIA, AttributeValue.fromS(avaliacao.urgencia().name()),
                AtributosAvaliacao.DATA_ENVIO, AttributeValue.fromS(avaliacao.dataEnvio()),
                AtributosAvaliacao.CRIADO_EM, AttributeValue.fromS(avaliacao.criadoEm()));
    }
}
