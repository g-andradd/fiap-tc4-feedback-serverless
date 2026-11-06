package br.com.fiap.feedback.notificacao;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.AtributosAvaliacao;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.AvisoCritico;
import br.com.fiap.feedback.dominio.ItemInvalidoException;
import br.com.fiap.feedback.dominio.Urgencia;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.security.Security;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.crac.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsBuilder;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Consome o Stream da tabela de avaliações e publica um aviso no tópico de administradores para cada
 * avaliação crítica criada.
 *
 * <p>O lote é percorrido em ordem. Na primeira falha ao publicar, a função devolve o número de
 * sequência daquele registro e para, porque o Lambda retoma dele e publicar os seguintes só geraria
 * duplicatas. Registro que não seja criação de item crítico é ignorado, e item fora do contrato é
 * descartado: tentar de novo não o conserta e atrasaria os avisos seguintes do shard.
 *
 * <p>Dados do aluno não vão para log nem para trace, em nível nenhum. Exceção é registrada só pela
 * classe e, se vier do serviço, pelo código de erro e pelo ID da requisição.
 *
 * <p>As linhas que representam falha da função levam o campo {@code alarme} com o valor
 * {@code FALHA_FUNCAO}, que o filtro de métrica do alarme de falha procura: a falha ao publicar,
 * inclusive quando só parte do lote falha, o item descartado e o {@code Error} que escapa da
 * invocação. As métricas só são descarregadas quando a invocação contou alguma, porque o Powertools
 * grava um WARN a cada descarga vazia.
 */
public class NotificarCriticaHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse> {

    // Precisa vir antes do logger: a JVM guarda o valor na primeira resolução de nome.
    static {
        Security.setProperty("networkaddress.cache.ttl", "0");
        TracingUtils.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(NotificarCriticaHandler.class);

    private static final String METRICA_ITEM_INVALIDO = "ItemInvalido";
    private static final String DESCONHECIDO = "desconhecido";
    private static final String CAMPO_ALARME = "alarme";
    private static final String MARCADOR_ALARME = "FALHA_FUNCAO";

    // Com o Timeout de 30 s da função, uma falha na publicação encerra a tentativa pelo catch, com a
    // sequência informada, em vez de um timeout que derrubaria o lote inteiro.
    private static final Duration TEMPO_MAXIMO_CHAMADA = Duration.ofSeconds(5);

    /**
     * Recursos de produção. A classe só é carregada pelo construtor público, que o runtime chama na
     * inicialização, então eles entram no snapshot do SnapStart. Os testes usam o outro construtor e
     * não dependem de região, credenciais nem rede. Criar o cliente não chama a AWS.
     *
     * <p>O aquecimento usa a mesma instância de cliente do handler. O contexto do CRaC guarda só
     * referência fraca, por isso a instância fica neste campo estático.
     */
    private static final class Recursos {
        static final SnsClient CLIENTE = criarCliente(SnsClient.builder());
        static final String TOPICO = Objects.requireNonNull(
                System.getenv("TOPICO_ADMINS_ARN"), "Variável TOPICO_ADMINS_ARN não definida.");
        static final Metrics METRICAS = MetricsBuilder.builder().build();
        static final Aquecimento AQUECIMENTO = new Aquecimento(CLIENTE, TOPICO);

        static {
            Core.getGlobalContext().register(AQUECIMENTO);
        }
    }

    private final SnsClient cliente;
    private final String topico;
    private final Metrics metricas;
    private final Rastreador rastreador;

    /** Construtor usado pelo runtime do Lambda. */
    public NotificarCriticaHandler() {
        this(Recursos.CLIENTE, Recursos.TOPICO, Recursos.METRICAS, Rastreador.XRAY);
    }

    NotificarCriticaHandler(SnsClient cliente, String topico, Metrics metricas, Rastreador rastreador) {
        this.cliente = cliente;
        this.topico = topico;
        this.metricas = metricas;
        this.rastreador = rastreador;
    }

    static SnsClient criarCliente(SnsClientBuilder construtor) {
        return construtor
                .httpClient(UrlConnectionHttpClient.create())
                .overrideConfiguration(configuracao -> configuracao.apiCallTimeout(TEMPO_MAXIMO_CHAMADA))
                .build();
    }

    /**
     * Trata um lote do Stream.
     *
     * @param evento registros do Stream
     * @param contexto contexto da invocação
     * @return lista vazia no sucesso, ou o número de sequência do primeiro registro que falhou
     */
    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent evento, Context contexto) {
        try {
            // Antes do withLogging, que marca o fim do cold start no estado global do Powertools.
            emitirMetrica(() -> metricas.captureColdStartMetric(contexto));
            return PowertoolsLogging.withLogging(contexto, () -> {
                Contagem contagem = new Contagem();
                try {
                    return processar(evento, contagem);
                } finally {
                    contagem.descarregar();
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

    private StreamsEventResponse processar(DynamodbEvent evento, Contagem contagem) {
        if (evento != null && evento.getRecords() != null) {
            for (DynamodbStreamRecord registro : evento.getRecords()) {
                if (!processarRegistro(registro, contagem)) {
                    return new StreamsEventResponse(
                            List.of(new BatchItemFailure(registro.getDynamodb().getSequenceNumber())));
                }
            }
        }
        return new StreamsEventResponse(List.of());
    }

    /** Devolve falso só quando a publicação falha, o único caso que precisa de nova tentativa. */
    private boolean processarRegistro(DynamodbStreamRecord registro, Contagem contagem) {
        if (!ehCriacaoCritica(registro)) {
            LOG.info("Registro ignorado",
                    entry("eventID", registro.getEventID()),
                    entry("eventName", registro.getEventName()));
            return true;
        }
        Avaliacao avaliacao;
        try {
            avaliacao = Avaliacao.deAtributos(paraAtributos(registro.getDynamodb().getNewImage()));
        } catch (ItemInvalidoException e) {
            descartar(registro, e, contagem);
            return true;
        }
        return publicar(registro, avaliacao);
    }

    // Segunda checagem depois do filtro do gatilho: um filtro não tem teste unitário, e a
    // documentação se contradiz sobre filtrar por eventName.
    private static boolean ehCriacaoCritica(DynamodbStreamRecord registro) {
        if (!OperationType.INSERT.toString().equals(registro.getEventName())) {
            return false;
        }
        StreamRecord dados = registro.getDynamodb();
        if (dados == null || dados.getNewImage() == null) {
            return false;
        }
        AttributeValue urgencia = dados.getNewImage().get(AtributosAvaliacao.URGENCIA);
        return urgencia != null && Urgencia.CRITICA.name().equals(urgencia.getS());
    }

    private boolean publicar(DynamodbStreamRecord registro, Avaliacao avaliacao) {
        AvisoCritico aviso = AvisoCritico.de(avaliacao);
        PublishRequest pedido = PublishRequest.builder()
                .topicArn(topico)
                .subject(aviso.assunto())
                .message(aviso.corpo())
                .build();
        try {
            rastreador.emSubsegmento("publicarAviso", () -> {
                rastreador.anotar(AtributosAvaliacao.ID, avaliacao.id());
                String idMensagem = cliente.publish(pedido).messageId();
                LOG.info("Aviso publicado",
                        entry(AtributosAvaliacao.ID, avaliacao.id()),
                        entry("eventID", registro.getEventID()),
                        entry("messageId", idMensagem));
            });
            return true;
        } catch (RuntimeException e) {
            registrarFalha(registro, avaliacao, e);
            return false;
        }
    }

    // Nunca passa a exceção ao logger: mensagem e pilha podem citar dados da avaliação.
    private void registrarFalha(DynamodbStreamRecord registro, Avaliacao avaliacao, RuntimeException e) {
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
        LOG.error("Falha ao publicar aviso",
                entry(AtributosAvaliacao.ID, avaliacao.id()),
                entry("eventID", registro.getEventID()),
                entry("sequencia", registro.getDynamodb().getSequenceNumber()),
                entry("classe", e.getClass().getName()),
                entry("codigoErro", codigoErro),
                entry("idRequisicao", idRequisicao),
                entry(CAMPO_ALARME, MARCADOR_ALARME));
    }

    private void descartar(DynamodbStreamRecord registro, ItemInvalidoException e, Contagem contagem) {
        LOG.error("Item inválido descartado",
                entry("eventID", registro.getEventID()),
                entry("campos", e.campos()),
                entry(CAMPO_ALARME, MARCADOR_ALARME));
        contagem.contar(METRICA_ITEM_INVALIDO);
    }

    /**
     * Métricas contadas numa invocação. Sem nenhuma, a descarga não é feita: o Powertools gravaria
     * {@code No metrics were emitted} em WARN a cada lote publicado com sucesso.
     */
    private final class Contagem {
        private boolean contou;

        void contar(String metrica) {
            emitirMetrica(() -> {
                metricas.addMetric(metrica, 1, MetricUnit.COUNT);
                contou = true;
            });
        }

        void descarregar() {
            if (contou) {
                emitirMetrica(metricas::flush);
            }
        }
    }

    // Falha de métrica não pode virar falha do registro: reprocessar duplicaria avisos já publicados.
    private void emitirMetrica(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            LOG.warn("Falha ao emitir métrica", entry("classe", e.getClass().getName()));
        }
    }

    // Lê cada atributo só pelo tipo do contrato do item: valor de outro tipo conta como ausente.
    private static Map<String, String> paraAtributos(Map<String, AttributeValue> imagem) {
        Map<String, String> atributos = new HashMap<>();
        imagem.forEach((nome, valor) -> {
            String texto = AtributosAvaliacao.NOTA.equals(nome) ? valor.getN() : valor.getS();
            if (texto != null) {
                atributos.put(nome, texto);
            }
        });
        return atributos;
    }
}
