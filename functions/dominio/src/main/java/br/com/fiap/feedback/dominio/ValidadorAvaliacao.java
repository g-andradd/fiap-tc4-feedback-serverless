package br.com.fiap.feedback.dominio;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de aceitação de {@code descricao} e {@code nota}.
 */
public final class ValidadorAvaliacao {

    /** Limite de {@code descricao}, em caracteres Unicode. Um emoji conta como um. */
    public static final int TAMANHO_MAXIMO_DESCRICAO = 1000;

    private static final BigInteger MENOR_NOTA = BigInteger.valueOf(Avaliacao.NOTA_MINIMA);
    private static final BigInteger MAIOR_NOTA = BigInteger.valueOf(Avaliacao.NOTA_MAXIMA);

    private ValidadorAvaliacao() {
    }

    /**
     * Valida os dois campos da entrada.
     *
     * @param descricao valor recebido em {@code descricao}
     * @param nota valor recebido em {@code nota}
     * @return nomes dos campos inválidos, vazia se a entrada for aceita
     */
    public static List<String> validar(ValorEntrada descricao, ValorEntrada nota) {
        List<String> invalidos = new ArrayList<>(2);
        if (!(descricao instanceof ValorEntrada.Texto(String texto) && descricaoValida(texto))) {
            invalidos.add(AtributosAvaliacao.DESCRICAO);
        }
        if (!(nota instanceof ValorEntrada.Inteiro(BigInteger inteiro) && notaValida(inteiro))) {
            invalidos.add(AtributosAvaliacao.NOTA);
        }
        return List.copyOf(invalidos);
    }

    static boolean descricaoValida(String descricao) {
        return temConteudo(descricao)
                && descricao.codePointCount(0, descricao.length()) <= TAMANHO_MAXIMO_DESCRICAO;
    }

    // String.isBlank não serve: considera branco outros caracteres Unicode que a spec não lista.
    private static boolean temConteudo(String texto) {
        return texto.chars().anyMatch(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r');
    }

    private static boolean notaValida(BigInteger nota) {
        return nota.compareTo(MENOR_NOTA) >= 0 && nota.compareTo(MAIOR_NOTA) <= 0;
    }
}
