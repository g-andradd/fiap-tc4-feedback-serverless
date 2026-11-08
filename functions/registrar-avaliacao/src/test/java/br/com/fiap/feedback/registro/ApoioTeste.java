package br.com.fiap.feedback.registro;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Eventos, contexto e rastreador falsos, sem contato com a AWS. */
final class ApoioTeste {

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

    static APIGatewayV2HTTPEvent evento(String corpo) {
        return APIGatewayV2HTTPEvent.builder().withBody(corpo).build();
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
                return 10_000;
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
