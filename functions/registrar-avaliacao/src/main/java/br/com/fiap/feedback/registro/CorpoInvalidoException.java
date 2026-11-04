package br.com.fiap.feedback.registro;

/**
 * Corpo da requisição que não é um objeto JSON bem formado. Guarda só um motivo sem dado de entrada
 * e não encadeia a exceção original nem a pilha, porque ambas podem citar trechos do corpo.
 */
final class CorpoInvalidoException extends Exception {

    CorpoInvalidoException(String motivo) {
        super(motivo, null, false, false);
    }
}
