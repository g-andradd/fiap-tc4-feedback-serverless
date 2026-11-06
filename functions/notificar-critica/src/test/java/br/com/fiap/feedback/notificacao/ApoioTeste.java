package br.com.fiap.feedback.notificacao;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Eventos, contexto e rastreador falsos, sem contato com a AWS. */
final class ApoioTeste {

    static final String DATA_ENVIO = "2026-11-03";
    static final String CRIADO_EM = "2026-11-03T15:04:05.000Z";

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

    private ApoioTeste() {
    }

    static DynamodbEvent lote(DynamodbStreamRecord... registros) {
        DynamodbEvent evento = new DynamodbEvent();
        evento.setRecords(Arrays.asList(registros));
        return evento;
    }

    /** Item com os seis atributos, nos tipos gravados por {@code registrar-avaliacao}. */
    static Map<String, AttributeValue> item(String id, String descricao, String urgencia) {
        Map<String, AttributeValue> imagem = new HashMap<>();
        imagem.put("id", new AttributeValue().withS(id));
        imagem.put("descricao", new AttributeValue().withS(descricao));
        imagem.put("nota", new AttributeValue().withN("2"));
        imagem.put("urgencia", new AttributeValue().withS(urgencia));
        imagem.put("dataEnvio", new AttributeValue().withS(DATA_ENVIO));
        imagem.put("criadoEm", new AttributeValue().withS(CRIADO_EM));
        return imagem;
    }

    static Map<String, AttributeValue> itemCritico(String id, String descricao) {
        return item(id, descricao, "CRITICA");
    }

    static DynamodbStreamRecord registro(String eventName, String sequencia, Map<String, AttributeValue> imagem) {
        DynamodbStreamRecord registro = new DynamodbStreamRecord();
        registro.setEventID("evento-" + sequencia);
        registro.setEventName(eventName);
        StreamRecord dados = new StreamRecord();
        dados.setSequenceNumber(sequencia);
        dados.setNewImage(imagem);
        registro.setDynamodb(dados);
        return registro;
    }

    static DynamodbStreamRecord insercao(String sequencia, Map<String, AttributeValue> imagem) {
        return registro("INSERT", sequencia, imagem);
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
                return 30_000;
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
