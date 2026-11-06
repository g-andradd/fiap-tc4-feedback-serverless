package br.com.fiap.feedback.relatorio;

import static br.com.fiap.feedback.relatorio.ApoioTeste.BUCKET;
import static br.com.fiap.feedback.relatorio.ApoioTeste.SEGUNDA_08H;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TABELA;
import static br.com.fiap.feedback.relatorio.ApoioTeste.TOPICO;
import static br.com.fiap.feedback.relatorio.ApoioTeste.contexto;
import static br.com.fiap.feedback.relatorio.ApoioTeste.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.DataReferenciaInvalidaException;
import br.com.fiap.feedback.relatorio.ApoioTeste.Cenario;
import br.com.fiap.feedback.relatorio.ApoioTeste.ObjetoGravado;
import com.amazonaws.services.lambda.runtime.Context;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.mockito.InOrder;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

class GerarRelatorioSemanalHandlerTest {

    private static final String CHAVE_SEGUNDA = "relatorios/2026-11-09.txt";
    private static final Instant TERCA_MEIO_DIA = Instant.parse("2026-11-03T15:00:00Z");

    private static final String NUMEROS_NOTAS_2_5_8 = """
            Ambiente: dev
            Data de referência: 2026-11-09
            Janela: 2026-11-02 a 2026-11-08

            Média das notas: 5,00

            Quantidade por urgência
            CRITICA: 1
            MEDIA: 1
            BAIXA: 1

            Quantidade por dia
            2026-11-02: 0
            2026-11-03: 2
            2026-11-04: 0
            2026-11-05: 1
            2026-11-06: 0
            2026-11-07: 0
            2026-11-08: 0
            """;

    private static final String LOCALIZACAO_SEGUNDA = """
            Bucket: feedback-relatorios-123456789012-dev
            Chave: relatorios/2026-11-09.txt
            Localização: s3://feedback-relatorios-123456789012-dev/relatorios/2026-11-09.txt

            O bucket é privado. Acesse com uma credencial da conta.
            """;

    private Cenario cenario;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
    }

    private void comAvaliacoesDe2E5E8(String descricao) {
        cenario.dynamo.adicionar(item("a", descricao, 2, "CRITICA", "2026-11-03"));
        cenario.dynamo.adicionar(item("b", descricao, 5, "MEDIA", "2026-11-03"));
        cenario.dynamo.adicionar(item("c", descricao, 8, "BAIXA", "2026-11-05"));
    }

    private String textoGravado(String chave) {
        return cenario.s3.objeto(chave).texto();
    }

    private String avisoPublicado() {
        assertThat(cenario.sns.publicacoes).hasSize(1);
        return cenario.sns.publicacoes.getFirst().message();
    }

    private List<String> diasConsultados() {
        return cenario.dynamo.consultas.stream()
                .map(consulta -> consulta.expressionAttributeValues().get(":dia").s())
                .toList();
    }

    private static DynamoDbException erroDynamo() {
        return (DynamoDbException) DynamoDbException.builder().message("falha").statusCode(500).build();
    }

    private void assertFalhouSemGravarNemAvisar(Runnable execucao) {
        assertThatThrownBy(execucao::run)
                .isInstanceOf(GeracaoRelatorioException.class)
                .hasNoCause();
        assertThat(cenario.s3.gravacoes).isZero();
        assertThat(cenario.sns.gravacoesAntesDePublicar).isEmpty();
    }

    @Nested
    @DisplayName("Geração automática semanal")
    class GeracaoAutomatica {

        @Test
        @DisplayName("Execução agendada em 09/11/2026 às 08:00 usa a referência 2026-11-09")
        void execucaoAgendada() {
            Map<String, Object> resposta = cenario.executarNaSegunda();

            assertThat(resposta)
                    .containsEntry("dataReferencia", "2026-11-09")
                    .containsEntry("chave", CHAVE_SEGUNDA)
                    .containsEntry("quantidadeAvaliacoes", 0);
            assertThat(cenario.s3.objeto(CHAVE_SEGUNDA)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Janela de apuração")
    class Janela {

        @Test
        @DisplayName("Janela de uma segunda-feira: uma consulta por dia, em ordem, no índice por-data")
        void janelaDeUmaSegunda() {
            cenario.executarNaSegunda();

            assertThat(diasConsultados()).containsExactly(
                    "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05",
                    "2026-11-06", "2026-11-07", "2026-11-08");
            assertThat(cenario.dynamo.consultas).allSatisfy(consulta -> {
                assertThat(consulta.tableName()).isEqualTo(TABELA);
                assertThat(consulta.indexName()).isEqualTo("por-data");
                assertThat(consulta.keyConditionExpression()).isEqualTo("dataEnvio = :dia");
                assertThat(consulta.filterExpression()).isNull();
                assertThat(consulta.limit()).isNull();
            });
        }

        @Test
        @DisplayName("Avaliações no primeiro e no último dia da janela entram")
        void extremosEntram() {
            cenario.dynamo.adicionar(item("a", "no-primeiro-dia", 5, "MEDIA", "2026-11-02"));
            cenario.dynamo.adicionar(item("b", "no-ultimo-dia", 5, "MEDIA", "2026-11-08"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("no-primeiro-dia", "no-ultimo-dia", "Avaliações (2)");
        }

        @Test
        @DisplayName("Avaliações no dia da referência e oito dias antes não aparecem nem contam")
        void foraDaJanela() {
            cenario.dynamo.adicionar(item("a", "no-dia-da-referencia", 0, "CRITICA", "2026-11-09"));
            cenario.dynamo.adicionar(item("b", "oito-dias-antes", 0, "CRITICA", "2026-11-01"));
            cenario.dynamo.adicionar(item("c", "dentro", 8, "BAIXA", "2026-11-05"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA))
                    .doesNotContain("no-dia-da-referencia", "oito-dias-antes")
                    .contains("Avaliações (1)", "Média das notas: 8,00", "CRITICA: 0");
        }

        private void adicionarEnviadaEm(String descricao, String instante) {
            Avaliacao avaliacao = Avaliacao.nova(descricao, 5, descricao, Instant.parse(instante));
            cenario.dynamo.adicionar(item(
                    avaliacao.id(), descricao, 5, "MEDIA", avaliacao.dataEnvio(), avaliacao.criadoEm()));
        }

        @Test
        @DisplayName("Limite final avaliado no fuso de São Paulo")
        void limiteFinal() {
            adicionarEnviadaEm("as-23h59-de-08", "2026-11-09T02:59:00Z");
            adicionarEnviadaEm("as-00h00-de-09", "2026-11-09T03:00:00Z");

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("as-23h59-de-08").doesNotContain("as-00h00-de-09");
        }

        @Test
        @DisplayName("Limite inicial avaliado no fuso de São Paulo")
        void limiteInicial() {
            adicionarEnviadaEm("as-23h59-de-01", "2026-11-02T02:59:00Z");

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).doesNotContain("as-23h59-de-01").contains("Avaliações (0)");
        }
    }

    @Nested
    @DisplayName("Conteúdo do relatório")
    class Conteudo {

        @Test
        @DisplayName("Semana com avaliações: texto completo do objeto gravado")
        void semanaComAvaliacoes() {
            cenario.dynamo.adicionar(item("a", "Aula confusa", 2, "CRITICA", "2026-11-03"));
            cenario.dynamo.adicionar(item("b", "Aula regular", 5, "MEDIA", "2026-11-03"));
            cenario.dynamo.adicionar(item("c", "Aula ótima", 8, "BAIXA", "2026-11-05"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).isEqualTo("RELATÓRIO SEMANAL DE AVALIAÇÕES\n"
                    + NUMEROS_NOTAS_2_5_8
                    + """

                    Avaliações (3)

                    Data de envio: 2026-11-03 | Urgência: CRITICA
                    Descrição: Aula confusa

                    Data de envio: 2026-11-03 | Urgência: MEDIA
                    Descrição: Aula regular

                    Data de envio: 2026-11-05 | Urgência: BAIXA
                    Descrição: Aula ótima
                    """);
        }

        @Test
        @DisplayName("Média não exata")
        void mediaNaoExata() {
            cenario.dynamo.adicionar(item("a", "x", 5, "MEDIA", "2026-11-03"));
            cenario.dynamo.adicionar(item("b", "x", 6, "MEDIA", "2026-11-03"));
            cenario.dynamo.adicionar(item("c", "x", 6, "MEDIA", "2026-11-04"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("Média das notas: 5,67\n");
        }

        @Test
        @DisplayName("Urgência vem do que foi persistido, sem recalcular a partir da nota")
        void urgenciaPersistida() {
            cenario.dynamo.adicionar(item("a", "x", 9, "CRITICA", "2026-11-03"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA))
                    .contains("Urgência: CRITICA", "CRITICA: 1", "BAIXA: 0")
                    .doesNotContain("Urgência: BAIXA");
        }
    }

    @Nested
    @DisplayName("Totais completos")
    class TotaisCompletos {

        @Test
        @DisplayName("Dias e urgências sem avaliações aparecem com zero")
        void diasEUrgenciasComZero() {
            cenario.dynamo.adicionar(item("a", "x", 8, "BAIXA", "2026-11-03"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("""
                    Quantidade por urgência
                    CRITICA: 0
                    MEDIA: 0
                    BAIXA: 1

                    Quantidade por dia
                    2026-11-02: 0
                    2026-11-03: 1
                    2026-11-04: 0
                    2026-11-05: 0
                    2026-11-06: 0
                    2026-11-07: 0
                    2026-11-08: 0
                    """);
        }
    }

    @Nested
    @DisplayName("Semana sem avaliações")
    class SemanaVazia {

        @Test
        @DisplayName("Janela vazia grava relatório sem valor de média e avisa os administradores")
        void janelaVazia() {
            Map<String, Object> resposta = cenario.executarNaSegunda();

            assertThat(resposta).containsEntry("quantidadeAvaliacoes", 0);
            assertThat(textoGravado(CHAVE_SEGUNDA))
                    .contains("Média das notas: sem avaliações na janela\n", "CRITICA: 0", "MEDIA: 0", "BAIXA: 0")
                    .contains("Avaliações (0)\n")
                    .doesNotContain("Data de envio:");
            assertThat(avisoPublicado())
                    .contains("Média das notas: sem avaliações na janela\n")
                    .contains("CRITICA: 0", "MEDIA: 0", "BAIXA: 0")
                    .contains("2026-11-02: 0", "2026-11-08: 0")
                    .contains("s3://" + BUCKET + "/" + CHAVE_SEGUNDA);
        }

        @Test
        @DisplayName("Média zero não se confunde com ausência de média")
        void mediaZero() {
            cenario.dynamo.adicionar(item("a", "x", 0, "CRITICA", "2026-11-03"));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA))
                    .contains("Média das notas: 0,00\n")
                    .doesNotContain("sem avaliações na janela");
        }
    }

    @Nested
    @DisplayName("Guarda do relatório")
    class Guarda {

        @Test
        @DisplayName("Relatório vai para o bucket configurado, em texto UTF-8, na chave da data")
        void relatorioGuardado() {
            cenario.dynamo.adicionar(item("a", "Avaliação ótima", 8, "BAIXA", "2026-11-03"));

            cenario.executarNaSegunda();

            ObjetoGravado objeto = cenario.s3.objeto(CHAVE_SEGUNDA);
            assertThat(objeto.requisicao().bucket()).isEqualTo(BUCKET);
            assertThat(objeto.requisicao().key()).isEqualTo(CHAVE_SEGUNDA);
            assertThat(objeto.requisicao().contentType()).isEqualTo("text/plain; charset=utf-8");
            assertThat(objeto.requisicao().acl()).isNull();
            assertThat(objeto.texto()).contains("Avaliação ótima");
        }
    }

    @Nested
    @DisplayName("Aviso aos administradores")
    class Aviso {

        @Test
        @DisplayName("Aviso sem descrições e sem notas: o texto é comparado por inteiro")
        void textoCompletoSemDescricaoNemNota() {
            comAvaliacoesDe2E5E8("TEXTO-MARCADOR-AVISO");

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("TEXTO-MARCADOR-AVISO");
            assertThat(avisoPublicado())
                    .doesNotContain("TEXTO-MARCADOR-AVISO")
                    .isEqualTo("Relatório semanal gerado.\n\n" + NUMEROS_NOTAS_2_5_8 + "\n" + LOCALIZACAO_SEGUNDA);
        }

        @Test
        @DisplayName("Aviso vai para o tópico configurado, com a data de referência no assunto")
        void topicoEAssunto() {
            cenario.executarNaSegunda();

            assertThat(cenario.sns.publicacoes).singleElement().satisfies(publicacao -> {
                assertThat(publicacao.topicArn()).isEqualTo(TOPICO);
                assertThat(publicacao.subject()).isEqualTo("Relatório semanal 2026-11-09");
            });
        }

        @Test
        @DisplayName("A localização do aviso corresponde ao relatório que acabou de ser guardado")
        void localizacaoCorresponde() {
            cenario.executarNaSegunda();

            ObjetoGravado objeto = cenario.s3.objetos.values().iterator().next();
            assertThat(avisoPublicado()).contains(
                    "Data de referência: 2026-11-09\n",
                    "Localização: s3://" + objeto.requisicao().bucket() + "/" + objeto.requisicao().key() + "\n");
        }
    }

    @Nested
    @DisplayName("Acionamento manual")
    class AcionamentoManual {

        @Test
        @DisplayName("Sem parâmetro em 03/11/2026: referência do dia, janela anterior, relatório e aviso")
        void semParametro() {
            Map<String, Object> resposta = cenario.executar(TERCA_MEIO_DIA, Map.of());

            assertThat(resposta).containsEntry("dataReferencia", "2026-11-03");
            assertThat(diasConsultados()).startsWith("2026-10-27").endsWith("2026-11-02");
            assertThat(textoGravado("relatorios/2026-11-03.txt")).contains("Janela: 2026-10-27 a 2026-11-02\n");
            assertThat(avisoPublicado()).contains("Data de referência: 2026-11-03\n");
        }

        @Test
        @DisplayName("Às 22:00 de 03/11 em São Paulo, quando em UTC já é 04/11, a referência é 03/11")
        void diaAtualEmSaoPaulo() {
            Map<String, Object> resposta = cenario.executar(Instant.parse("2026-11-04T01:00:00Z"), Map.of());

            assertThat(resposta).containsEntry("dataReferencia", "2026-11-03");
        }

        @Test
        @DisplayName("Avaliação do próprio dia fica fora sem parâmetro")
        void proprioDiaFora() {
            cenario.dynamo.adicionar(item("a", "de-hoje", 5, "MEDIA", "2026-11-03"));

            cenario.executar(TERCA_MEIO_DIA, Map.of());

            assertThat(textoGravado("relatorios/2026-11-03.txt")).doesNotContain("de-hoje");
        }

        private static Stream<Map<String, Object>> eventosSemData() {
            Map<String, Object> comNulo = new HashMap<>();
            comNulo.put("dataReferencia", null);
            return Stream.of(null, Map.of(), comNulo, Map.of("outroCampo", "ignorado"));
        }

        @ParameterizedTest
        @MethodSource("eventosSemData")
        @DisplayName("Evento nulo, vazio, com data nula ou sem o campo usa o dia atual")
        void eventoSemData(Map<String, Object> evento) {
            assertThat(cenario.executar(TERCA_MEIO_DIA, evento)).containsEntry("dataReferencia", "2026-11-03");
        }

        @Test
        @DisplayName("A data é calculada a cada invocação, nunca na inicialização")
        void dataCalculadaACadaInvocacao() {
            GerarRelatorioSemanalHandler primeiro = cenario.handler(TERCA_MEIO_DIA);
            GerarRelatorioSemanalHandler segundo = cenario.handler(SEGUNDA_08H);

            assertThat(primeiro.handleRequest(Map.of(), contexto())).containsEntry("dataReferencia", "2026-11-03");
            assertThat(segundo.handleRequest(Map.of(), contexto())).containsEntry("dataReferencia", "2026-11-09");
        }
    }

    @Nested
    @DisplayName("Data de referência opcional")
    class DataOpcional {

        @Test
        @DisplayName("Referência de amanhã inclui a avaliação de hoje")
        void referenciaDeAmanha() {
            cenario.dynamo.adicionar(item("a", "de-hoje", 5, "MEDIA", "2026-11-03"));

            cenario.executar(TERCA_MEIO_DIA, Map.of("dataReferencia", "2026-11-04"));

            assertThat(textoGravado("relatorios/2026-11-04.txt"))
                    .contains("Janela: 2026-10-28 a 2026-11-03\n", "de-hoje");
        }

        @Test
        @DisplayName("Referência no passado")
        void referenciaNoPassado() {
            cenario.executar(TERCA_MEIO_DIA, Map.of("dataReferencia", "2026-10-26"));

            assertThat(textoGravado("relatorios/2026-10-26.txt")).contains("Janela: 2026-10-19 a 2026-10-25\n");
        }

        private static Stream<Object> valoresInvalidos() {
            return Stream.of("09/11/2026", "2026-02-30", "", "+12026-01-01", 20261109, true, List.of("2026-11-09"));
        }

        @ParameterizedTest(name = "valor {0}")
        @MethodSource("valoresInvalidos")
        @DisplayName("Valor inválido é rejeitado informando o motivo, sem nenhuma chamada a serviço")
        void valorInvalido(Object valor) {
            assertThatThrownBy(() -> cenario.executar(SEGUNDA_08H, Map.of("dataReferencia", valor)))
                    .isInstanceOf(DataReferenciaInvalidaException.class)
                    .hasMessageContaining("Data de referência inválida");

            assertThat(cenario.dynamo.consultas).isEmpty();
            cenario.semNenhumEfeito();
            assertThat(cenario.s3.objetos).isEmpty();
        }

        @Test
        @DisplayName("Rejeição preserva relatório existente")
        void rejeicaoPreservaRelatorio() {
            cenario.dynamo.adicionar(item("a", "x", 5, "MEDIA", "2026-11-03"));
            cenario.executarPara("2026-11-09");
            ObjetoGravado antes = cenario.s3.objeto(CHAVE_SEGUNDA);

            assertThatThrownBy(() -> cenario.executarPara("2026-02-30"))
                    .isInstanceOf(DataReferenciaInvalidaException.class);

            assertThat(cenario.s3.objetos).hasSize(1);
            assertThat(cenario.s3.objeto(CHAVE_SEGUNDA)).isSameAs(antes);
            assertThat(cenario.s3.gravacoes).isEqualTo(1);
            assertThat(cenario.sns.publicacoes).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Reexecução")
    class Reexecucao {

        @Test
        @DisplayName("Com avaliação nova: mesma chave, um único objeto, com a avaliação nova")
        void comAvaliacaoNova() {
            cenario.dynamo.adicionar(item("a", "antiga", 5, "MEDIA", "2026-11-03"));
            cenario.executarPara("2026-11-09");
            assertThat(textoGravado(CHAVE_SEGUNDA)).doesNotContain("registrada-entre-as-execucoes");

            cenario.dynamo.adicionar(item("b", "registrada-entre-as-execucoes", 5, "MEDIA", "2026-11-08"));
            cenario.executarPara("2026-11-09");

            assertThat(cenario.s3.gravacoes).isEqualTo(2);
            assertThat(cenario.s3.objetos).containsOnlyKeys(BUCKET + "/" + CHAVE_SEGUNDA);
            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("antiga", "registrada-entre-as-execucoes");
        }

        @Test
        @DisplayName("Manual seguida de agendada na mesma data: um relatório, com o resultado da agendada")
        void manualSeguidaDeAgendada() {
            cenario.executar(TERCA_MEIO_DIA, Map.of("dataReferencia", "2026-11-09"));
            cenario.dynamo.adicionar(item("a", "antes-da-agendada", 5, "MEDIA", "2026-11-08"));

            cenario.executarNaSegunda();

            assertThat(cenario.s3.objetos).containsOnlyKeys(BUCKET + "/" + CHAVE_SEGUNDA);
            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("antes-da-agendada");
        }

        @Test
        @DisplayName("Aviso da reexecução traz a localização do relatório vigente")
        void avisoDaReexecucao() {
            cenario.executarPara("2026-11-09");
            cenario.executarPara("2026-11-09");

            assertThat(cenario.sns.publicacoes).hasSize(2).allSatisfy(publicacao ->
                    assertThat(publicacao.message()).contains("s3://" + BUCKET + "/" + CHAVE_SEGUNDA));
        }

        @Test
        @DisplayName("Datas de referência diferentes convivem, cada uma com a sua janela")
        void datasDiferentes() {
            cenario.executarPara("2026-11-04");
            cenario.executarPara("2026-11-09");

            assertThat(cenario.s3.objetos).hasSize(2);
            assertThat(textoGravado("relatorios/2026-11-04.txt")).contains("Janela: 2026-10-28 a 2026-11-03\n");
            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("Janela: 2026-11-02 a 2026-11-08\n");
        }
    }

    @Nested
    @DisplayName("Leitura das avaliações")
    class Leitura {

        @Test
        @DisplayName("Várias páginas são lidas por inteiro, na ordem, antes de qualquer gravação")
        void variasPaginas() {
            cenario.dynamo.definirPaginas(
                    "2026-11-03",
                    List.of(item("a", "pagina-1", 2, "CRITICA", "2026-11-03", "2026-11-03T10:00:00.000Z")),
                    List.of(item("b", "pagina-2", 5, "MEDIA", "2026-11-03", "2026-11-03T11:00:00.000Z")),
                    List.of(item("c", "pagina-3", 8, "BAIXA", "2026-11-03", "2026-11-03T12:00:00.000Z")));

            Map<String, Object> resposta = cenario.executarNaSegunda();

            assertThat(resposta).containsEntry("quantidadeAvaliacoes", 3);
            assertThat(diasConsultados().stream().filter("2026-11-03"::equals)).hasSize(3);
            assertThat(textoGravado(CHAVE_SEGUNDA)).containsSubsequence("pagina-1", "pagina-2", "pagina-3");
            List<QueryRequest> doDia = new ArrayList<>(cenario.dynamo.consultas.subList(1, 4));
            assertThat(doDia.getFirst().hasExclusiveStartKey()).isFalse();
            assertThat(doDia.get(1).exclusiveStartKey()).isNotEmpty();
            assertThat(doDia.get(2).exclusiveStartKey()).isNotEmpty();
        }

        @Test
        @DisplayName("Página vazia no meio não encerra a leitura do dia")
        void paginaVaziaNoMeio() {
            cenario.dynamo.definirPaginas(
                    "2026-11-03",
                    List.of(),
                    List.of(item("a", "depois-da-vazia", 5, "MEDIA", "2026-11-03")));

            cenario.executarNaSegunda();

            assertThat(textoGravado(CHAVE_SEGUNDA)).contains("depois-da-vazia");
        }

        @Test
        @DisplayName("Falha em todos os dias: sem gravação, sem aviso, execução em falha")
        void falhaEmTodosOsDias() {
            for (String dia : List.of(
                    "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06", "2026-11-07", "2026-11-08")) {
                cenario.dynamo.falharEm(dia, 0, erroDynamo());
            }

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
            assertThat(cenario.dynamo.consultas).hasSize(1);
        }

        @Test
        @DisplayName("Falha em um dos 7 dias: sem gravação, sem aviso, execução em falha")
        void falhaEmUmDia() {
            cenario.dynamo.adicionar(item("a", "x", 5, "MEDIA", "2026-11-03"));
            cenario.dynamo.falharEm("2026-11-05", 0, erroDynamo());

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
            assertThat(diasConsultados()).endsWith("2026-11-05");
        }

        @Test
        @DisplayName("Leitura interrompida no meio de um dia: sem gravação, sem aviso, execução em falha")
        void falhaNoMeioDeUmDia() {
            cenario.dynamo.definirPaginas(
                    "2026-11-05",
                    List.of(item("a", "x", 5, "MEDIA", "2026-11-05")),
                    List.of(item("b", "x", 5, "MEDIA", "2026-11-05")));
            cenario.dynamo.falharEm("2026-11-05", 1, erroDynamo());

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
        }

        private Map<String, AttributeValue> itemSem(String atributo) {
            Map<String, AttributeValue> item = item("a", "x", 5, "MEDIA", "2026-11-05");
            item.remove(atributo);
            return item;
        }

        @ParameterizedTest(name = "sem {0}")
        @ValueSource(strings = {"id", "descricao", "nota", "urgencia", "criadoEm"})
        @DisplayName("Item sem atributo obrigatório encerra como leitura incompleta")
        void itemSemAtributo(String atributo) {
            cenario.dynamo.definirPaginas("2026-11-05", List.of(itemSem(atributo)));

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
        }

        @Test
        @DisplayName("Item com urgência desconhecida encerra como leitura incompleta")
        void urgenciaDesconhecida() {
            cenario.dynamo.adicionar(item("a", "x", 5, "ALTA", "2026-11-05"));

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
        }

        @Test
        @DisplayName("Item com nota de outro tipo encerra como leitura incompleta")
        void notaDeOutroTipo() {
            Map<String, AttributeValue> item = item("a", "x", 5, "MEDIA", "2026-11-05");
            item.put("nota", AttributeValue.fromS("5"));
            cenario.dynamo.adicionar(item);

            assertFalhouSemGravarNemAvisar(cenario::executarNaSegunda);
        }
    }

    @Nested
    @DisplayName("Falha na gravação")
    class FalhaNaGravacao {

        @Test
        @DisplayName("Gravação falha: nenhum aviso e execução em falha")
        void gravacaoFalha() {
            cenario.s3.falha = S3Exception.builder().message("negado").statusCode(403).build();

            assertThatThrownBy(cenario::executarNaSegunda)
                    .isInstanceOf(GeracaoRelatorioException.class)
                    .hasNoCause();

            assertThat(cenario.s3.gravacoes).isEqualTo(1);
            assertThat(cenario.sns.gravacoesAntesDePublicar).isEmpty();
        }
    }

    @Nested
    @DisplayName("Falha na publicação")
    class FalhaNaPublicacao {

        @Test
        @DisplayName("Publicação falha depois da gravação: relatório guardado e execução em falha")
        void publicacaoFalha() {
            cenario.sns.falha = SnsException.builder().message("negado").statusCode(403).build();

            assertThatThrownBy(cenario::executarNaSegunda)
                    .isInstanceOf(GeracaoRelatorioException.class)
                    .hasNoCause();

            assertThat(cenario.s3.objeto(CHAVE_SEGUNDA)).isNotNull();
            assertThat(cenario.sns.gravacoesAntesDePublicar).containsExactly(1);
        }

        @Test
        @DisplayName("Reexecução depois da falha: um relatório, aviso recebido e execução com sucesso")
        void reexecucaoDepoisDaFalha() {
            cenario.sns.falha = SnsException.builder().message("negado").statusCode(403).build();
            assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

            cenario.sns.falha = null;
            Map<String, Object> resposta = cenario.executarNaSegunda();

            assertThat(resposta).containsEntry("chave", CHAVE_SEGUNDA);
            assertThat(cenario.s3.objetos).containsOnlyKeys(BUCKET + "/" + CHAVE_SEGUNDA);
            assertThat(avisoPublicado()).contains("s3://" + BUCKET + "/" + CHAVE_SEGUNDA);
        }
    }

    @Nested
    @DisplayName("Ordem dos efeitos")
    class Ordem {

        @Test
        @DisplayName("Lê, grava e só então publica")
        void leGravaPublica() {
            cenario.executarNaSegunda();

            assertThat(cenario.rastreador.subsegmentos)
                    .containsExactly("lerAvaliacoes", "gravarRelatorio", "publicarAviso");
            assertThat(cenario.sns.gravacoesAntesDePublicar).containsExactly(1);
            assertThat(cenario.rastreador.anotacoes).containsOnly(Map.entry("dataReferencia", "2026-11-09"));
        }
    }

    @Nested
    @DisplayName("Métricas")
    class Metricas {

        @Test
        @DisplayName("Sucesso conta e descarrega no fim")
        void sucesso() {
            Context contexto = contexto();

            cenario.handler(SEGUNDA_08H).handleRequest(Map.of(), contexto);

            InOrder ordem = inOrder(cenario.metricas);
            ordem.verify(cenario.metricas).captureColdStartMetric(contexto);
            ordem.verify(cenario.metricas).addMetric("RelatorioGerado", 1, MetricUnit.COUNT);
            ordem.verify(cenario.metricas).flush();
        }

        @Test
        @DisplayName("Falha conta como erro interno e descarrega")
        void falha() {
            cenario.s3.falha = new IllegalStateException("falha");

            assertThatThrownBy(cenario::executarNaSegunda).isInstanceOf(GeracaoRelatorioException.class);

            verify(cenario.metricas).addMetric("ErroInterno", 1, MetricUnit.COUNT);
            verify(cenario.metricas, never()).addMetric(eq("RelatorioGerado"), anyDouble(), any(MetricUnit.class));
            verify(cenario.metricas).flush();
        }

        @Test
        @DisplayName("Data rejeitada conta à parte e descarrega")
        void rejeicao() {
            assertThatThrownBy(() -> cenario.executarPara("hoje")).isInstanceOf(DataReferenciaInvalidaException.class);

            verify(cenario.metricas).addMetric("DataReferenciaRejeitada", 1, MetricUnit.COUNT);
            verify(cenario.metricas).flush();
        }

        @Test
        @DisplayName("Falha de métrica não troca o resultado da execução")
        void falhaDeMetrica() {
            doThrow(new IllegalStateException("sem namespace")).when(cenario.metricas).captureColdStartMetric(any(Context.class));
            doThrow(new IllegalStateException("sem namespace"))
                    .when(cenario.metricas).addMetric(anyString(), anyDouble(), any(MetricUnit.class));
            doThrow(new IllegalStateException("sem namespace")).when(cenario.metricas).flush();

            assertThat(cenario.executarNaSegunda()).containsEntry("chave", CHAVE_SEGUNDA);
            assertThat(cenario.sns.publicacoes).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Inicialização com SnapStart")
    class Inicializacao {

        @Test
        @DisplayName("Cache de DNS desligado ao carregar a classe")
        void cacheDeDns() {
            cenario.handler(SEGUNDA_08H);

            assertThat(Security.getProperty("networkaddress.cache.ttl")).isEqualTo("0");
        }

        @Test
        @DisplayName("Runtime encontra o construtor público sem argumentos")
        void construtorPublico() throws Exception {
            assertThat(GerarRelatorioSemanalHandler.class.getConstructor()).isNotNull();
        }

        @Test
        @DisplayName("Os três clientes reais usam o cliente HTTP escolhido e desistem da chamada em 10 segundos")
        void tempoMaximoDaChamada() {
            Duration esperado = Duration.ofSeconds(10);
            AnonymousCredentialsProvider credenciais = AnonymousCredentialsProvider.create();

            try (DynamoDbClient dynamo = GerarRelatorioSemanalHandler.criarCliente(
                            DynamoDbClient.builder().region(Region.SA_EAST_1).credentialsProvider(credenciais));
                    S3Client s3 = GerarRelatorioSemanalHandler.criarCliente(
                            S3Client.builder().region(Region.SA_EAST_1).credentialsProvider(credenciais));
                    SnsClient sns = GerarRelatorioSemanalHandler.criarCliente(
                            SnsClient.builder().region(Region.SA_EAST_1).credentialsProvider(credenciais))) {
                assertThat(dynamo.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(esperado);
                assertThat(s3.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(esperado);
                assertThat(sns.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                        .contains(esperado);
            }
        }
    }
}
