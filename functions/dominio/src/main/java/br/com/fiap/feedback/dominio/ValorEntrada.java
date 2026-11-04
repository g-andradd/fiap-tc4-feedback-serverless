package br.com.fiap.feedback.dominio;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Valor de um campo do corpo da requisição, já classificado pelo adaptador e sem depender da
 * biblioteca de JSON. A distinção é o que permite rejeitar {@code "7"}, {@code 7.0} e {@code 1e1}.
 */
public sealed interface ValorEntrada {

    /** Campo ausente do corpo ou igual a {@code null}. */
    record Ausente() implements ValorEntrada {
    }

    /**
     * Campo do tipo texto.
     *
     * @param valor texto como recebido
     */
    record Texto(String valor) implements ValorEntrada {
        public Texto {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /**
     * Número escrito como inteiro, de qualquer tamanho.
     *
     * @param valor número recebido
     */
    record Inteiro(BigInteger valor) implements ValorEntrada {
        public Inteiro {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /** Qualquer outro tipo: número com fração ou expoente, booleano, objeto ou lista. */
    record Outro() implements ValorEntrada {
    }
}
