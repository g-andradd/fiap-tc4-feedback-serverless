package br.com.fiap.feedback.relatorio;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.AvisoRelatorio;
import br.com.fiap.feedback.dominio.ConsolidadorRelatorio;
import br.com.fiap.feedback.dominio.DataReferencia;
import br.com.fiap.feedback.dominio.FormatadorRelatorio;
import br.com.fiap.feedback.dominio.JanelaApuracao;
import br.com.fiap.feedback.dominio.LocalizacaoRelatorio;
import br.com.fiap.feedback.dominio.RelatorioSemanal;
import br.com.fiap.feedback.dominio.ResumoSemanal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Caso de uso da função: lê, consolida, grava e publica, nessa ordem. Cada passo só roda se o
 * anterior tiver sucesso, então uma falha na leitura não grava, uma falha na gravação não avisa e uma
 * falha na publicação deixa o relatório guardado. A reexecução corrige, porque a chave é a mesma.
 *
 * <p>Descrição, nota individual e média não vão para log nem para trace.
 */
final class GeradorRelatorio {

    /** O que foi guardado, para o handler montar a resposta. */
    record Resultado(LocalizacaoRelatorio localizacao, ResumoSemanal resumo) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(GeradorRelatorio.class);
    private static final String TIPO_DO_CONTEUDO = "text/plain; charset=utf-8";
    private static final String DATA_REFERENCIA = "dataReferencia";

    private final LeitorAvaliacoes leitor;
    private final S3Client s3;
    private final SnsClient sns;
    private final String ambiente;
    private final String bucket;
    private final String topicoArn;
    private final Rastreador rastreador;

    GeradorRelatorio(
            LeitorAvaliacoes leitor,
            S3Client s3,
            SnsClient sns,
            String ambiente,
            String bucket,
            String topicoArn,
            Rastreador rastreador) {
        this.leitor = leitor;
        this.s3 = s3;
        this.sns = sns;
        this.ambiente = ambiente;
        this.bucket = bucket;
        this.topicoArn = topicoArn;
        this.rastreador = rastreador;
    }

    Resultado gerar(DataReferencia referencia) {
        List<Avaliacao> avaliacoes = ler(referencia);

        RelatorioSemanal relatorio = ConsolidadorRelatorio.consolidar(ambiente, referencia, avaliacoes);
        ResumoSemanal resumo = relatorio.resumo();
        LocalizacaoRelatorio localizacao = LocalizacaoRelatorio.de(bucket, referencia);

        gravar(referencia, localizacao, FormatadorRelatorio.formatar(relatorio));
        publicar(referencia, AvisoRelatorio.de(resumo, localizacao));

        LOG.info("Relatório gerado",
                entry(DATA_REFERENCIA, referencia.toString()),
                entry("quantidadeTotal", resumo.quantidadeTotal()),
                entry("totaisPorUrgencia", resumo.totaisPorUrgencia()),
                entry("bucket", localizacao.bucket()),
                entry("chave", localizacao.chave()));
        return new Resultado(localizacao, resumo);
    }

    private List<Avaliacao> ler(DataReferencia referencia) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        emSubsegmento("lerAvaliacoes", referencia,
                () -> avaliacoes.addAll(leitor.ler(JanelaApuracao.de(referencia))));
        return avaliacoes;
    }

    private void gravar(DataReferencia referencia, LocalizacaoRelatorio localizacao, String texto) {
        emSubsegmento("gravarRelatorio", referencia, () -> s3.putObject(
                PutObjectRequest.builder()
                        .bucket(localizacao.bucket())
                        .key(localizacao.chave())
                        .contentType(TIPO_DO_CONTEUDO)
                        .build(),
                RequestBody.fromString(texto, StandardCharsets.UTF_8)));
        // Separado do log final: se a publicação falhar, este registro mostra que o objeto ficou guardado.
        LOG.info("Relatório gravado",
                entry(DATA_REFERENCIA, referencia.toString()),
                entry("bucket", localizacao.bucket()),
                entry("chave", localizacao.chave()));
    }

    private void publicar(DataReferencia referencia, AvisoRelatorio aviso) {
        emSubsegmento("publicarAviso", referencia, () -> sns.publish(PublishRequest.builder()
                .topicArn(topicoArn)
                .subject(aviso.assunto())
                .message(aviso.corpo())
                .build()));
    }

    private void emSubsegmento(String nome, DataReferencia referencia, Runnable acao) {
        rastreador.emSubsegmento(nome, () -> {
            rastreador.anotar(DATA_REFERENCIA, referencia.toString());
            acao.run();
        });
    }
}
