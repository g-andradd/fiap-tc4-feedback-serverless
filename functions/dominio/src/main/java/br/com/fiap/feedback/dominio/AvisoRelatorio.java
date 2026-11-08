package br.com.fiap.feedback.dominio;

import java.util.Objects;

/**
 * Aviso aos administradores de que um relatório foi guardado. Traz os números da semana e a
 * localização. Como é montado só com {@link ResumoSemanal}, descrições e notas individuais ficam de
 * fora por construção.
 *
 * @param assunto assunto da mensagem
 * @param corpo texto da mensagem
 */
public record AvisoRelatorio(String assunto, String corpo) {

    /**
     * Exige os dois componentes.
     *
     * @throws NullPointerException se algum for nulo
     */
    public AvisoRelatorio {
        Objects.requireNonNull(assunto, "assunto");
        Objects.requireNonNull(corpo, "corpo");
    }

    /**
     * Monta o aviso.
     *
     * @param resumo números da semana
     * @param localizacao localização do relatório guardado
     * @return aviso pronto para publicar
     */
    public static AvisoRelatorio de(ResumoSemanal resumo, LocalizacaoRelatorio localizacao) {
        String corpo = "Relatório semanal gerado.\n\n"
                + FormatadorRelatorio.numeros(resumo)
                + "\nBucket: " + localizacao.bucket()
                + "\nChave: " + localizacao.chave()
                + "\nLocalização: " + localizacao.uri()
                + "\n\nO bucket é privado. Acesse com uma credencial da conta.\n";
        return new AvisoRelatorio("Relatório semanal " + resumo.dataReferencia(), corpo);
    }
}
