package br.com.fiap.feedback.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvisoRelatorioTest {

    private static final DataReferencia REFERENCIA = new DataReferencia(LocalDate.of(2026, 11, 9));
    private static final String BUCKET = "feedback-relatorios-123456789012-dev";

    private static Avaliacao avaliacao(String descricao, int nota, String dataEnvio) {
        return new Avaliacao(
                "id-" + nota, descricao, nota, Urgencia.deNota(nota), dataEnvio, dataEnvio + "T12:00:00.000Z");
    }

    private static AvisoRelatorio aviso(Avaliacao... avaliacoes) {
        ResumoSemanal resumo =
                ConsolidadorRelatorio.consolidar("dev", REFERENCIA, Arrays.asList(avaliacoes)).resumo();
        return AvisoRelatorio.de(resumo, LocalizacaoRelatorio.de(BUCKET, REFERENCIA));
    }

    @Test
    @DisplayName("Texto completo com notas 2, 5 e 8: nenhuma nota individual aparece")
    void textoCompleto() {
        AvisoRelatorio aviso = aviso(
                avaliacao("TEXTO-MARCADOR-AVISO", 2, "2026-11-03"),
                avaliacao("TEXTO-MARCADOR-AVISO", 5, "2026-11-03"),
                avaliacao("TEXTO-MARCADOR-AVISO", 8, "2026-11-05"));

        assertThat(aviso.assunto()).isEqualTo("Relatório semanal 2026-11-09");
        assertThat(aviso.corpo()).isEqualTo("""
                Relatório semanal gerado.

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

                Bucket: feedback-relatorios-123456789012-dev
                Chave: relatorios/2026-11-09.txt
                Localização: s3://feedback-relatorios-123456789012-dev/relatorios/2026-11-09.txt

                O bucket é privado. Acesse com uma credencial da conta.
                """);
    }

    @Test
    @DisplayName("Semana vazia: zeros, o texto no lugar da média e a localização")
    void semanaVazia() {
        assertThat(aviso().corpo()).isEqualTo("""
                Relatório semanal gerado.

                Ambiente: dev
                Data de referência: 2026-11-09
                Janela: 2026-11-02 a 2026-11-08

                Média das notas: sem avaliações na janela

                Quantidade por urgência
                CRITICA: 0
                MEDIA: 0
                BAIXA: 0

                Quantidade por dia
                2026-11-02: 0
                2026-11-03: 0
                2026-11-04: 0
                2026-11-05: 0
                2026-11-06: 0
                2026-11-07: 0
                2026-11-08: 0

                Bucket: feedback-relatorios-123456789012-dev
                Chave: relatorios/2026-11-09.txt
                Localização: s3://feedback-relatorios-123456789012-dev/relatorios/2026-11-09.txt

                O bucket é privado. Acesse com uma credencial da conta.
                """);
    }

    @Test
    @DisplayName("A descrição não aparece no aviso")
    void semDescricao() {
        assertThat(aviso(avaliacao("TEXTO-MARCADOR-AVISO", 5, "2026-11-03")).corpo())
                .doesNotContain("TEXTO-MARCADOR-AVISO");
    }

    @Test
    @DisplayName("A localização do aviso corresponde à chave gravada")
    void localizacaoCorrespondeAChave() {
        LocalizacaoRelatorio localizacao = LocalizacaoRelatorio.de(BUCKET, REFERENCIA);

        AvisoRelatorio aviso = AvisoRelatorio.de(
                ConsolidadorRelatorio.consolidar("dev", REFERENCIA, List.of()).resumo(), localizacao);

        assertThat(localizacao.chave()).isEqualTo("relatorios/2026-11-09.txt");
        assertThat(localizacao.uri()).isEqualTo("s3://" + BUCKET + "/relatorios/2026-11-09.txt");
        assertThat(aviso.corpo())
                .contains("Bucket: " + localizacao.bucket() + "\n")
                .contains("Chave: " + localizacao.chave() + "\n")
                .contains("Localização: " + localizacao.uri() + "\n");
    }

    @Test
    @DisplayName("A mesma data gera a mesma chave, e datas diferentes geram chaves diferentes")
    void chavePorData() {
        DataReferencia outra = new DataReferencia(LocalDate.of(2026, 11, 4));

        assertThat(LocalizacaoRelatorio.de(BUCKET, REFERENCIA)).isEqualTo(LocalizacaoRelatorio.de(BUCKET, REFERENCIA));
        assertThat(LocalizacaoRelatorio.de(BUCKET, outra).chave()).isEqualTo("relatorios/2026-11-04.txt");
    }

    @Test
    @DisplayName("O aviso só aceita resumo e localização, sem acesso à lista de avaliações")
    void recebeSoResumoELocalizacao() throws Exception {
        assertThat(AvisoRelatorio.class.getMethod("de", ResumoSemanal.class, LocalizacaoRelatorio.class))
                .isNotNull();
        assertThat(ResumoSemanal.class.getRecordComponents())
                .noneMatch(componente -> componente.getType().equals(List.class)
                        || componente.getType().equals(RelatorioSemanal.class));
    }
}
