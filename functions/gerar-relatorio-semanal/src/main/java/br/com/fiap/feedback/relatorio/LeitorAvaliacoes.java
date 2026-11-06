package br.com.fiap.feedback.relatorio;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.AtributosAvaliacao;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.ItemInvalidoException;
import br.com.fiap.feedback.dominio.JanelaApuracao;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Lê as avaliações da janela pelo índice {@code por-data}, com uma consulta por dia. Qualquer falha,
 * em qualquer página, sobe para quem chamou: a lista só fica pronta depois das 7 consultas, então
 * nunca existe resultado parcial.
 */
final class LeitorAvaliacoes {

    static final String INDICE = "por-data";

    private static final Logger LOG = LoggerFactory.getLogger(LeitorAvaliacoes.class);
    private static final String DIA = ":dia";
    private static final String CONDICAO = AtributosAvaliacao.DATA_ENVIO + " = " + DIA;

    private final DynamoDbClient cliente;
    private final String tabela;

    LeitorAvaliacoes(DynamoDbClient cliente, String tabela) {
        this.cliente = cliente;
        this.tabela = tabela;
    }

    List<Avaliacao> ler(JanelaApuracao janela) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        for (LocalDate dia : janela.dias()) {
            QueryRequest consulta = QueryRequest.builder()
                    .tableName(tabela)
                    .indexName(INDICE)
                    .keyConditionExpression(CONDICAO)
                    .expressionAttributeValues(Map.of(DIA, AttributeValue.fromS(dia.toString())))
                    .build();
            // O paginador percorre todas as páginas, porque cada resposta traz no máximo 1 MB.
            cliente.queryPaginator(consulta).items().forEach(item -> avaliacoes.add(converter(item)));
        }
        return List.copyOf(avaliacoes);
    }

    // Item inválido é leitura incompleta: seguir sem ele publicaria números errados.
    private static Avaliacao converter(Map<String, AttributeValue> item) {
        try {
            return Avaliacao.deAtributos(paraTexto(item));
        } catch (ItemInvalidoException e) {
            LOG.error("Item de avaliação inválido",
                    entry(AtributosAvaliacao.ID, idDoItem(item)),
                    entry("campos", e.campos()));
            throw e;
        }
    }

    // Atributo de outro tipo devolve null no acessor e fica de fora, como se estivesse ausente.
    private static Map<String, String> paraTexto(Map<String, AttributeValue> item) {
        Map<String, String> atributos = new HashMap<>();
        copiar(item, atributos, AtributosAvaliacao.ID, AttributeValue::s);
        copiar(item, atributos, AtributosAvaliacao.DESCRICAO, AttributeValue::s);
        copiar(item, atributos, AtributosAvaliacao.NOTA, AttributeValue::n);
        copiar(item, atributos, AtributosAvaliacao.URGENCIA, AttributeValue::s);
        copiar(item, atributos, AtributosAvaliacao.DATA_ENVIO, AttributeValue::s);
        copiar(item, atributos, AtributosAvaliacao.CRIADO_EM, AttributeValue::s);
        return atributos;
    }

    private static void copiar(
            Map<String, AttributeValue> item,
            Map<String, String> atributos,
            String nome,
            Function<AttributeValue, String> leitura) {
        AttributeValue valor = item.get(nome);
        if (valor != null && leitura.apply(valor) != null) {
            atributos.put(nome, leitura.apply(valor));
        }
    }

    private static String idDoItem(Map<String, AttributeValue> item) {
        AttributeValue id = item.get(AtributosAvaliacao.ID);
        return id == null || id.s() == null ? "desconhecido" : id.s();
    }
}
