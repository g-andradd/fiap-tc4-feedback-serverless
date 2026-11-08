package br.com.fiap.feedback.dominio;

/**
 * Urgência de uma avaliação, derivada da nota. Esta é a única tabela de classificação do projeto.
 */
public enum Urgencia {
    CRITICA,
    MEDIA,
    BAIXA;

    private static final int MAIOR_NOTA_CRITICA = 3;
    private static final int MAIOR_NOTA_MEDIA = 6;

    /**
     * Classifica uma nota.
     *
     * @param nota inteiro de 0 a 10
     * @return {@link #CRITICA} de 0 a 3, {@link #MEDIA} de 4 a 6 e {@link #BAIXA} de 7 a 10
     * @throws IllegalArgumentException se a nota estiver fora do intervalo
     */
    public static Urgencia deNota(int nota) {
        if (nota < Avaliacao.NOTA_MINIMA || nota > Avaliacao.NOTA_MAXIMA) {
            throw new IllegalArgumentException("Nota fora do intervalo permitido.");
        }
        if (nota <= MAIOR_NOTA_CRITICA) {
            return CRITICA;
        }
        return nota <= MAIOR_NOTA_MEDIA ? MEDIA : BAIXA;
    }
}
