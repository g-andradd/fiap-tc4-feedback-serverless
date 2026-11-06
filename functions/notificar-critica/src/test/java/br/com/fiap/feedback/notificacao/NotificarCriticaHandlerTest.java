package br.com.fiap.feedback.notificacao;

import static br.com.fiap.feedback.notificacao.ApoioTeste.contexto;
import static br.com.fiap.feedback.notificacao.ApoioTeste.insercao;
import static br.com.fiap.feedback.notificacao.ApoioTeste.item;
import static br.com.fiap.feedback.notificacao.ApoioTeste.itemCritico;
import static br.com.fiap.feedback.notificacao.ApoioTeste.lote;
import static br.com.fiap.feedback.notificacao.ApoioTeste.registro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.feedback.notificacao.ApoioTeste.RastreadorFalso;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

class NotificarCriticaHandlerTest {

    private static final String TOPICO = "arn:aws:sns:sa-east-1:111122223333:feedback-admins-dev";
    private static final PublishResponse ACEITA = PublishResponse.builder().messageId("mensagem-1").build();
    private static final RuntimeException FALHA = SdkClientException.create("Tópico indisponível");

    private SnsClient cliente;
    private Metrics metricas;
    private RastreadorFalso rastreador;
    private NotificarCriticaHandler handler;

    @BeforeEach
    void preparar() {
        cliente = mock(SnsClient.class);
        metricas = mock(Metrics.class);
        rastreador = new RastreadorFalso();
        handler = new NotificarCriticaHandler(cliente, TOPICO, metricas, rastreador);
        when(cliente.publish(any(PublishRequest.class))).thenReturn(ACEITA);
    }

    private StreamsEventResponse processar(DynamodbEvent evento) {
        return handler.handleRequest(evento, contexto());
    }

    private List<PublishRequest> publicados(int quantidade) {
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(cliente, times(quantidade)).publish(captor.capture());
        return captor.getAllValues();
    }

    private PublishRequest unicoPublicado() {
        return publicados(1).getFirst();
    }

    private void assertNadaPublicado() {
        verify(cliente, never()).publish(any(PublishRequest.class));
    }

    private static List<String> falhas(StreamsEventResponse resposta) {
        return resposta.getBatchItemFailures().stream().map(BatchItemFailure::getItemIdentifier).toList();
    }

    @Nested
    @DisplayName("Aviso imediato de avaliação crítica")
    class AvisoImediato {

        @Test
        @DisplayName("Aviso publicado no tópico do ambiente, na própria execução")
        void publicaNoTopicoConfigurado() {
            processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            PublishRequest pedido = unicoPublicado();
            assertThat(pedido.topicArn()).isEqualTo(TOPICO);
            assertThat(pedido.subject()).isEqualTo("Avaliação crítica registrada");
        }

        @Test
        @DisplayName("Lote com três críticas gera três publicações, uma para cada avaliação, em ordem")
        void loteComTresCriticas() {
            StreamsEventResponse resposta = processar(lote(
                    insercao("100", itemCritico("id-1", "Primeira descrição")),
                    insercao("101", itemCritico("id-2", "Segunda descrição")),
                    insercao("102", itemCritico("id-3", "Terceira descrição"))));

            List<PublishRequest> pedidos = publicados(3);
            assertThat(pedidos.get(0).message()).endsWith("Primeira descrição");
            assertThat(pedidos.get(1).message()).endsWith("Segunda descrição");
            assertThat(pedidos.get(2).message()).endsWith("Terceira descrição");
            assertThat(falhas(resposta)).isEmpty();
        }

        @Test
        @DisplayName("Sucesso devolve batchItemFailures vazio, não nulo")
        void sucessoSemFalhas() {
            StreamsEventResponse resposta = processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            assertThat(resposta.getBatchItemFailures()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Evento sem registros, ou nulo, não publica nem falha")
        void eventoSemRegistros() {
            assertThat(falhas(processar(new DynamodbEvent()))).isEmpty();
            assertThat(falhas(processar(lote()))).isEmpty();
            assertThat(falhas(processar(null))).isEmpty();
            assertNadaPublicado();
        }
    }

    @Nested
    @DisplayName("Conteúdo obrigatório do aviso")
    class Conteudo {

        @Test
        @DisplayName("Corpo com descrição, urgência e data de envio do item")
        void camposObrigatorios() {
            processar(lote(insercao("100", itemCritico("id-1", "Professor não apareceu"))));

            assertThat(unicoPublicado().message())
                    .contains("Professor não apareceu", "Urgência: CRITICA", "Data de envio: 2026-11-03");
        }

        @Test
        @DisplayName("Descrição de 1000 caracteres com acentos chega inteira, sem corte nem reticências")
        void descricaoNoTamanhoMaximo() {
            String descricao = "çãéíõúâ".repeat(142) + "àêôüèì";
            assertThat(descricao.codePointCount(0, descricao.length())).isEqualTo(1000);

            processar(lote(insercao("100", itemCritico("id-1", descricao))));

            assertThat(unicoPublicado().message())
                    .endsWith("Descrição:\n" + descricao)
                    .doesNotContain("...", "…");
        }

        @Test
        @DisplayName("Data de envio vem da avaliação, não da data do processamento")
        void dataEnvioPersistida() {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula ruim");
            imagem.put("dataEnvio", new AttributeValue().withS("2020-02-29"));
            imagem.put("criadoEm", new AttributeValue().withS("2020-03-01T02:59:59.000Z"));
            String hoje = DateTimeFormatter.ISO_LOCAL_DATE.format(
                    Instant.now().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate());

            processar(lote(insercao("100", imagem)));

            assertThat(unicoPublicado().message())
                    .contains("Data de envio: 2020-02-29")
                    .doesNotContain(hoje, "2020-03-01");
        }
    }

    @Nested
    @DisplayName("Aviso somente na criação da avaliação")
    class SomenteCriacao {

        @Test
        @DisplayName("Alteração de item crítico não gera aviso")
        void alteracaoDeItemCritico() {
            StreamsEventResponse resposta =
                    processar(lote(registro("MODIFY", "100", itemCritico("id-1", "Aula ruim"))));

            assertNadaPublicado();
            assertThat(falhas(resposta)).isEmpty();
        }

        @Test
        @DisplayName("Alteração que torna o item crítico não gera aviso, mesmo com imagem nova CRITICA")
        void alteracaoQueTornaCritico() {
            // Com Stream NEW_IMAGE não há imagem antiga: MEDIA alterado para CRITICA chega assim.
            processar(lote(registro("MODIFY", "100", item("id-1", "Aula ruim", "CRITICA"))));

            assertNadaPublicado();
        }

        @Test
        @DisplayName("Remoção não gera aviso, sem imagem nova como no Stream real")
        void remocao() {
            StreamsEventResponse resposta = processar(lote(registro("REMOVE", "100", null)));

            assertNadaPublicado();
            assertThat(falhas(resposta)).isEmpty();
        }

        @Test
        @DisplayName("Remoção não gera aviso, mesmo se vier com imagem crítica")
        void remocaoComImagem() {
            processar(lote(registro("REMOVE", "100", itemCritico("id-1", "Aula ruim"))));

            assertNadaPublicado();
        }

        @Test
        @DisplayName("Em lote misto, só a criação gera aviso")
        void loteMisto() {
            StreamsEventResponse resposta = processar(lote(
                    registro("MODIFY", "100", itemCritico("id-1", "Alterada")),
                    insercao("101", itemCritico("id-2", "Criada")),
                    registro("REMOVE", "102", null)));

            assertThat(unicoPublicado().message()).endsWith("Criada");
            assertThat(falhas(resposta)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Urgência diferente de CRITICA")
    class Urgencia {

        @ParameterizedTest
        @ValueSource(strings = {"MEDIA", "BAIXA", "critica", "URGENTE", ""})
        @DisplayName("Inserção com outra urgência não gera aviso e conta como processada")
        void outraUrgencia(String urgencia) {
            StreamsEventResponse resposta = processar(lote(insercao("100", item("id-1", "Aula", urgencia))));

            assertNadaPublicado();
            assertThat(falhas(resposta)).isEmpty();
        }

        @Test
        @DisplayName("Inserção sem o atributo urgencia não gera aviso")
        void semUrgencia() {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula");
            imagem.remove("urgencia");

            assertThat(falhas(processar(lote(insercao("100", imagem))))).isEmpty();
            assertNadaPublicado();
        }

        @Test
        @DisplayName("Urgência de outro tipo que não texto não gera aviso")
        void urgenciaDeOutroTipo() {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula");
            imagem.put("urgencia", new AttributeValue().withN("1"));

            assertThat(falhas(processar(lote(insercao("100", imagem))))).isEmpty();
            assertNadaPublicado();
        }

        @Test
        @DisplayName("Inserção sem dados do Stream não gera aviso nem falha")
        void semDadosDoStream() {
            DynamodbStreamRecord registro = new DynamodbStreamRecord();
            registro.setEventName("INSERT");

            assertThat(falhas(processar(lote(registro)))).isEmpty();
            assertNadaPublicado();
        }
    }

    @Nested
    @DisplayName("Falha na publicação não é sucesso silencioso")
    class FalhaNaPublicacao {

        private static Stream<RuntimeException> falhasDePublicacao() {
            return Stream.of(
                    SnsException.builder()
                            .message("Rejeitada")
                            .statusCode(403)
                            .awsErrorDetails(AwsErrorDetails.builder().errorCode("AuthorizationError").build())
                            .build(),
                    SdkClientException.create("Tópico indisponível"),
                    ApiCallTimeoutException.builder().message("Tempo esgotado").build(),
                    new IllegalStateException("Inesperada"));
        }

        @ParameterizedTest
        @MethodSource("falhasDePublicacao")
        @DisplayName("Publicação rejeitada ou tópico indisponível: o evento não é reportado como sucesso")
        void publicacaoRejeitada(RuntimeException falha) {
            when(cliente.publish(any(PublishRequest.class))).thenThrow(falha);

            StreamsEventResponse resposta = processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            assertThat(falhas(resposta)).containsExactly("100");
        }

        @Test
        @DisplayName("Falha no segundo de três: devolve a sequência dele e não publica o terceiro")
        void falhaNoSegundoRegistro() {
            when(cliente.publish(any(PublishRequest.class))).thenReturn(ACEITA).thenThrow(FALHA);

            StreamsEventResponse resposta = processar(lote(
                    insercao("100", itemCritico("id-1", "Primeira")),
                    insercao("101", itemCritico("id-2", "Segunda")),
                    insercao("102", itemCritico("id-3", "Terceira"))));

            assertThat(falhas(resposta)).containsExactly("101");
            List<PublishRequest> pedidos = publicados(2);
            assertThat(pedidos.get(0).message()).endsWith("Primeira");
            assertThat(pedidos.get(1).message()).endsWith("Segunda");
        }

        @Test
        @DisplayName("Falha no primeiro para o laço na hora")
        void falhaNoPrimeiroRegistro() {
            when(cliente.publish(any(PublishRequest.class))).thenThrow(FALHA);

            StreamsEventResponse resposta = processar(lote(
                    insercao("100", itemCritico("id-1", "Primeira")),
                    insercao("101", itemCritico("id-2", "Segunda"))));

            assertThat(falhas(resposta)).containsExactly("100");
            assertThat(publicados(1)).hasSize(1);
        }

        @Test
        @DisplayName("Evento reprocessado: quando o tópico volta, o aviso é publicado")
        void eventoReprocessado() {
            when(cliente.publish(any(PublishRequest.class))).thenThrow(FALHA).thenReturn(ACEITA);
            DynamodbEvent evento = lote(insercao("100", itemCritico("id-1", "Aula ruim")));

            assertThat(falhas(processar(evento))).containsExactly("100");
            assertThat(falhas(processar(evento))).isEmpty();

            List<PublishRequest> pedidos = publicados(2);
            assertThat(pedidos.get(1).message()).isEqualTo(pedidos.get(0).message());
        }

        @Test
        @DisplayName("Falha ao publicar não leva a métrica de item inválido")
        void falhaNaoContaComoItemInvalido() {
            when(cliente.publish(any(PublishRequest.class))).thenThrow(FALHA);

            processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            verify(metricas, never()).addMetric(anyString(), anyDouble(), any(MetricUnit.class));
        }
    }

    @Nested
    @DisplayName("Item fora do contrato")
    class ItemInvalido {

        @ParameterizedTest
        @ValueSource(strings = {"id", "descricao", "nota", "dataEnvio", "criadoEm"})
        @DisplayName("Item sem campo obrigatório é descartado sem falhar o lote")
        void itemSemCampo(String campo) {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula ruim");
            imagem.remove(campo);

            StreamsEventResponse resposta = processar(lote(insercao("100", imagem)));

            assertThat(falhas(resposta)).isEmpty();
            assertNadaPublicado();
            verify(metricas).addMetric("ItemInvalido", 1, MetricUnit.COUNT);
        }

        @Test
        @DisplayName("Item inválido não impede o aviso dos registros seguintes")
        void seguintesContinuam() {
            Map<String, AttributeValue> semDescricao = itemCritico("id-1", "ignorada");
            semDescricao.remove("descricao");

            StreamsEventResponse resposta = processar(lote(
                    insercao("100", semDescricao),
                    insercao("101", itemCritico("id-2", "Válida"))));

            assertThat(falhas(resposta)).isEmpty();
            assertThat(unicoPublicado().message()).endsWith("Válida");
        }

        @Test
        @DisplayName("Descrição só com brancos é item inválido")
        void descricaoEmBranco() {
            assertThat(falhas(processar(lote(insercao("100", itemCritico("id-1", "   ")))))).isEmpty();

            assertNadaPublicado();
            verify(metricas).addMetric("ItemInvalido", 1, MetricUnit.COUNT);
        }

        @Test
        @DisplayName("Descrição gravada como número é item inválido")
        void descricaoDeOutroTipo() {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "ignorada");
            imagem.put("descricao", new AttributeValue().withN("123"));

            assertThat(falhas(processar(lote(insercao("100", imagem))))).isEmpty();

            assertNadaPublicado();
            verify(metricas).addMetric("ItemInvalido", 1, MetricUnit.COUNT);
        }

        @Test
        @DisplayName("Nota gravada como texto é item inválido")
        void notaDeOutroTipo() {
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula ruim");
            imagem.put("nota", new AttributeValue().withS("2"));

            assertThat(falhas(processar(lote(insercao("100", imagem))))).isEmpty();

            assertNadaPublicado();
        }

        @Test
        @DisplayName("Registro ignorado não conta como item inválido")
        void ignoradoNaoContaComoInvalido() {
            processar(lote(insercao("100", item("id-1", "Aula", "MEDIA"))));

            verify(metricas, never()).addMetric(anyString(), anyDouble(), any(MetricUnit.class));
        }
    }

    @Nested
    @DisplayName("Entrega pelo menos uma vez, sem deduplicação")
    class SemDeduplicacao {

        @Test
        @DisplayName("Mesmo evento entregue duas vezes gera duas publicações, sem falha")
        void eventoRepetido() {
            DynamodbEvent evento = lote(insercao("100", itemCritico("id-1", "Aula ruim")));

            assertThat(falhas(processar(evento))).isEmpty();
            assertThat(falhas(processar(evento))).isEmpty();

            assertThat(publicados(2)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Avaliação registrada não depende do aviso")
    class RegistroIndependente {

        @Test
        @DisplayName("A função não tem cliente do DynamoDB no classpath, então não escreve na tabela")
        void semClienteDaTabela() {
            assertThatThrownBy(() -> Class.forName("software.amazon.awssdk.services.dynamodb.DynamoDbClient"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Métricas e rastreamento")
    class MetricasERastreamento {

        @Test
        @DisplayName("Publicação roda em subsegmento anotado com o id, sem descarregar métrica vazia")
        void sucesso() {
            processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            assertThat(rastreador.subsegmentos).containsExactly("publicarAviso");
            assertThat(rastreador.anotacoes).containsOnly(Map.entry("id", "id-1"));
            verify(metricas, never()).flush();
        }

        @Test
        @DisplayName("Lote só com registros ignorados não descarrega métrica vazia")
        void ignoradoNaoDescarrega() {
            processar(lote(registro("MODIFY", "100", itemCritico("id-1", "Aula"))));
            processar(lote());
            processar(null);

            verify(metricas, never()).flush();
        }

        @Test
        @DisplayName("Falha ao publicar não conta métrica, então não descarrega")
        void falhaNaoDescarrega() {
            when(cliente.publish(any(PublishRequest.class))).thenThrow(FALHA);

            processar(lote(insercao("100", itemCritico("id-1", "Aula ruim"))));

            verify(metricas, never()).flush();
        }

        @Test
        @DisplayName("Item descartado conta a métrica e descarrega uma vez no fim da invocação")
        void descarteDescarrega() {
            Map<String, AttributeValue> semDescricao = itemCritico("id-1", "ignorada");
            semDescricao.remove("descricao");
            Map<String, AttributeValue> semNota = itemCritico("id-2", "ignorada");
            semNota.remove("nota");

            processar(lote(
                    insercao("100", semDescricao),
                    insercao("101", itemCritico("id-3", "Válida")),
                    insercao("102", semNota)));

            InOrder ordem = inOrder(metricas);
            ordem.verify(metricas, times(2)).addMetric("ItemInvalido", 1, MetricUnit.COUNT);
            ordem.verify(metricas).flush();
        }

        @Test
        @DisplayName("A contagem não passa de uma invocação para a seguinte")
        void contagemPorInvocacao() {
            Map<String, AttributeValue> semDescricao = itemCritico("id-1", "ignorada");
            semDescricao.remove("descricao");

            processar(lote(insercao("100", semDescricao)));
            processar(lote(insercao("101", itemCritico("id-2", "Válida"))));

            verify(metricas, times(1)).flush();
        }

        @Test
        @DisplayName("Falha ao descarregar métricas não troca a resposta")
        void falhaAoDescarregar() {
            doThrow(new IllegalStateException("flush")).when(metricas).flush();
            Map<String, AttributeValue> semDescricao = itemCritico("id-1", "ignorada");
            semDescricao.remove("descricao");

            StreamsEventResponse resposta = processar(lote(
                    insercao("100", semDescricao),
                    insercao("101", itemCritico("id-2", "Válida"))));

            assertThat(falhas(resposta)).isEmpty();
            assertThat(unicoPublicado()).isNotNull();
            verify(metricas).flush();
        }

        @Test
        @DisplayName("Falha ao contar item inválido não vira falha do registro nem descarga vazia")
        void falhaAoContar() {
            doThrow(new IllegalStateException("métrica"))
                    .when(metricas).addMetric(eq("ItemInvalido"), anyDouble(), any(MetricUnit.class));
            Map<String, AttributeValue> imagem = itemCritico("id-1", "Aula");
            imagem.remove("descricao");

            assertThat(falhas(processar(lote(insercao("100", imagem))))).isEmpty();
            verify(metricas, never()).flush();
        }

        @Test
        @DisplayName("Falha ao medir cold start não impede a publicação")
        void falhaNoColdStart() {
            doThrow(new IllegalStateException("cold start"))
                    .when(metricas).captureColdStartMetric(any(Context.class));

            assertThat(falhas(processar(lote(insercao("100", itemCritico("id-1", "Aula ruim")))))).isEmpty();
            assertThat(unicoPublicado()).isNotNull();
        }

        @Test
        @DisplayName("Registro ignorado não abre subsegmento nem toca no cliente")
        void ignoradoSemSubsegmento() {
            SnsClient clienteIntacto = mock(SnsClient.class);
            NotificarCriticaHandler outro =
                    new NotificarCriticaHandler(clienteIntacto, TOPICO, metricas, rastreador);

            outro.handleRequest(lote(registro("MODIFY", "100", itemCritico("id-1", "Aula"))), contexto());

            assertThat(rastreador.subsegmentos).isEmpty();
            verifyNoInteractions(clienteIntacto);
        }
    }

    @Nested
    @DisplayName("Inicialização com SnapStart")
    class Inicializacao {

        @Test
        @DisplayName("Cache de DNS desligado ao carregar a classe")
        void cacheDeDns() {
            assertThat(Security.getProperty("networkaddress.cache.ttl")).isEqualTo("0");
        }

        @Test
        @DisplayName("Runtime encontra o construtor público sem argumentos")
        void construtorPublico() throws Exception {
            assertThat(NotificarCriticaHandler.class.getConstructor()).isNotNull();
        }

        @Test
        @DisplayName("O cliente desiste da chamada em 5 segundos, abaixo do Timeout da função")
        void tempoMaximoDaChamada() {
            SnsClientBuilder construtor = SnsClient.builder()
                    .region(Region.SA_EAST_1)
                    .credentialsProvider(AnonymousCredentialsProvider.create());

            try (SnsClient clienteReal = NotificarCriticaHandler.criarCliente(construtor)) {
                assertThat(clienteReal.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(Duration.ofSeconds(5));
            }
        }
    }
}
