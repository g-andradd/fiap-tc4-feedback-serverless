package br.com.fiap.feedback.dominio;

import java.util.Objects;

/**
 * Onde o relatório fica no bucket. A chave depende só da data de referência, então a reexecução
 * substitui o objeto e existe um único relatório por data.
 *
 * @param bucket nome do bucket de relatórios
 * @param chave chave do objeto
 */
public record LocalizacaoRelatorio(String bucket, String chave) {

    /**
     * Exige os dois componentes.
     *
     * @throws NullPointerException se algum for nulo
     */
    public LocalizacaoRelatorio {
        Objects.requireNonNull(bucket, "bucket");
        Objects.requireNonNull(chave, "chave");
    }

    /**
     * Calcula a localização do relatório de uma data.
     *
     * @param bucket nome do bucket de relatórios
     * @param dataReferencia data de referência
     * @return localização com a chave {@code relatorios/<dataReferencia>.txt}
     */
    public static LocalizacaoRelatorio de(String bucket, DataReferencia dataReferencia) {
        return new LocalizacaoRelatorio(bucket, "relatorios/" + dataReferencia + ".txt");
    }

    /**
     * Monta o endereço do objeto.
     *
     * @return endereço no formato {@code s3://<bucket>/<chave>}
     */
    public String uri() {
        return "s3://" + bucket + "/" + chave;
    }
}
