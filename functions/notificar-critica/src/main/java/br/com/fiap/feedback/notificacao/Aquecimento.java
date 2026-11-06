package br.com.fiap.feedback.notificacao;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import java.time.Duration;
import org.crac.Context;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;

/**
 * Hook do SnapStart que exercita o cliente do SNS antes do snapshot, para que credenciais, assinatura
 * e cliente HTTP já estejam carregados na primeira chamada depois da restauração.
 *
 * <p>Faz só um {@code GetTopicAttributes} no tópico de administradores, que lê atributos do tópico:
 * nada é publicado. Falha no aquecimento é registrada em WARN e não impede a publicação da versão. O
 * registro no contexto global fica com quem cria a instância, para o teste poder chamar o hook direto.
 */
final class Aquecimento implements Resource {

    private static final Logger LOG = LoggerFactory.getLogger(Aquecimento.class);

    private final SnsClient cliente;
    private final String topico;

    Aquecimento(SnsClient cliente, String topico) {
        this.cliente = cliente;
        this.topico = topico;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> contexto) {
        aquecer("SNS", "GetTopicAttributes",
                () -> cliente.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topico).build()));
    }

    @Override
    public void afterRestore(Context<? extends Resource> contexto) {
        // Nada a refazer: o cliente retoma as conexões e renova a credencial sozinho.
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
