package br.com.fiap.feedback.dominio;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos de data do item de avaliação. Os dois atributos saem do mesmo instante e são as chaves
 * do índice {@code por-data}.
 */
public final class CarimboDeTempo {

    /** Fuso usado para definir o dia da avaliação, o mesmo do relatório semanal. */
    public static final ZoneId ZONA_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    // Largura fixa porque o DynamoDB ordena texto por bytes: sem a fração, "...:59Z" ordenaria
    // depois de "...:59.123Z" e a ordem do índice deixaria de ser cronológica.
    private static final DateTimeFormatter FORMATO_CRIADO_EM =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter FORMATO_DATA_ENVIO =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT).withZone(ZONA_SAO_PAULO);

    private CarimboDeTempo() {
    }

    /**
     * Formata o instante de criação.
     *
     * @param instante instante do registro
     * @return ISO-8601 em UTC com 24 caracteres, com a fração truncada em milissegundos
     */
    public static String criadoEm(Instant instante) {
        return FORMATO_CRIADO_EM.format(instante);
    }

    /**
     * Calcula o dia do envio.
     *
     * @param instante instante do registro
     * @return dia em America/Sao_Paulo, no formato {@code yyyy-MM-dd}
     */
    public static String dataEnvio(Instant instante) {
        return FORMATO_DATA_ENVIO.format(instante);
    }
}
