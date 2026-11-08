package br.com.fiap.feedback.dominio;

import java.util.List;

/**
 * Item lido da tabela que não respeita o contrato de {@link Avaliacao}. Carrega só os nomes dos
 * campos, nunca os valores, para poder ser registrada em log sem expor dados do aluno.
 */
public final class ItemInvalidoException extends RuntimeException {

    private final List<String> campos;

    /**
     * Cria a exceção.
     *
     * @param campos nomes dos campos faltantes ou inválidos
     */
    public ItemInvalidoException(List<String> campos) {
        super("Item de avaliação inválido nos campos: " + String.join(", ", campos));
        this.campos = List.copyOf(campos);
    }

    /**
     * Devolve os campos rejeitados.
     *
     * @return nomes dos campos faltantes ou inválidos
     */
    public List<String> campos() {
        return campos;
    }
}
