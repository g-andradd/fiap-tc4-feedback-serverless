package br.com.fiap.feedback.relatorio;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.AtributosAvaliacao;
import java.time.Duration;
import java.util.Map;
import org.crac.Context;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;

/**
 * Hook do SnapStart que exercita os clientes do DynamoDB, do S3 e do SNS antes do snapshot, para que
 * credenciais, assinatura e cliente HTTP já estejam carregados na primeira chamada depois da
 * restauração.
 *
 * <p>Só faz leituras que não trazem dado do aluno: uma {@code Query} no índice {@code por-data} numa
 * partição que nunca existe, com limite de 1, um {@code HeadBucket} no bucket de relatórios e um
 * {@code GetTopicAttributes} no tópico de administradores. Nada é gravado nem publicado. Cada chamada
 * tem o próprio tratamento de falha, então a falha de um cliente não impede os outros, e nenhuma
 * falha impede a publicação da versão. O registro no contexto global fica com quem cria a instância,
 * para o teste poder chamar o hook direto.
 */
final class Aquecimento implements Resource {

    /** Valor de {@code dataEnvio} que nunca é gravado: a partição consultada fica sempre vazia. */
    static final String PARTICAO_VAZIA = "aquecimento";

    private static final Logger LOG = LoggerFactory.getLogger(Aquecimento.class);
    private static final String DIA = ":dia";

    private final DynamoDbClient dynamo;
    private final String tabela;
    private final S3Client s3;
    private final String bucket;
    private final SnsClient sns;
    private final String topicoArn;

    Aquecimento(DynamoDbClient dynamo, String tabela, S3Client s3, String bucket, SnsClient sns, String topicoArn) {
        this.dynamo = dynamo;
        this.tabela = tabela;
        this.s3 = s3;
        this.bucket = bucket;
        this.sns = sns;
        this.topicoArn = topicoArn;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> contexto) {
        aquecer("DynamoDB", "Query", () -> dynamo.query(QueryRequest.builder()
                .tableName(tabela)
                .indexName(LeitorAvaliacoes.INDICE)
                .keyConditionExpression(AtributosAvaliacao.DATA_ENVIO + " = " + DIA)
                .expressionAttributeValues(Map.of(DIA, AttributeValue.fromS(PARTICAO_VAZIA)))
                .limit(1)
                .build()));
        aquecer("S3", "HeadBucket", () -> s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build()));
        aquecer("SNS", "GetTopicAttributes",
                () -> sns.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topicoArn).build()));
    }

    @Override
    public void afterRestore(Context<? extends Resource> contexto) {
        // Nada a refazer: os clientes retomam as conexões e renovam a credencial sozinhos.
    }

    // LinkageError entra porque um NoClassDefFoundError aqui derrubaria a publicação da versão.
    // Nunca passa a exceção ao logger: mensagem e pilha não são controladas por este código.
    private static void aquecer(String servico, String operacao, Runnable chamada) {
        long inicio = System.nanoTime();
        try {
            chamada.run();
            LOG.info("Cliente aquecido",
                    entry("servico", servico),
                    entry("operacao", operacao),
                    entry("duracaoMs", Duration.ofNanos(System.nanoTime() - inicio).toMillis()));
        } catch (RuntimeException | LinkageError e) {
            registrarFalha(servico, e);
        }
    }

    private static void registrarFalha(String servico, Throwable e) {
        String codigoErro = codigoDoServico(e);
        if (codigoErro == null) {
            LOG.warn("Falha no aquecimento",
                    entry("servico", servico),
                    entry("classe", e.getClass().getName()));
        } else {
            LOG.warn("Falha no aquecimento",
                    entry("servico", servico),
                    entry("classe", e.getClass().getName()),
                    entry("codigoErro", codigoErro));
        }
    }

    private static String codigoDoServico(Throwable e) {
        if (e instanceof AwsServiceException servico) {
            AwsErrorDetails detalhes = servico.awsErrorDetails();
            return detalhes == null ? null : detalhes.errorCode();
        }
        return null;
    }
}
