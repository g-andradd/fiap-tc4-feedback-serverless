package br.com.fiap.feedback.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Avaliação de aula, com os seis atributos do item persistido.
 *
 * @param id identificador gerado pelo sistema
 * @param descricao texto enviado pelo aluno, como recebido
 * @param nota inteiro de 0 a 10
 * @param urgencia urgência calculada no registro
 * @param dataEnvio dia em America/Sao_Paulo, no formato {@code yyyy-MM-dd}
 * @param criadoEm instante em UTC, com largura fixa de 24 caracteres
 */
public record Avaliacao(
        String id,
        String descricao,
        int nota,
        Urgencia urgencia,
        String dataEnvio,
        String criadoEm) {

    /** Menor nota aceita. */
    public static final int NOTA_MINIMA = 0;

    /** Maior nota aceita. */
    public static final int NOTA_MAXIMA = 10;

    /**
     * Valida os atributos. As mensagens citam só o nome do campo, nunca o valor.
     *
     * @throws IllegalArgumentException se algum atributo violar o contrato
     */
    public Avaliacao {
        exigirTexto(id, AtributosAvaliacao.ID);
        exigirTexto(dataEnvio, AtributosAvaliacao.DATA_ENVIO);
        exigirTexto(criadoEm, AtributosAvaliacao.CRIADO_EM);
        if (descricao == null || !ValidadorAvaliacao.descricaoValida(descricao)) {
            throw new IllegalArgumentException(campoInvalido(AtributosAvaliacao.DESCRICAO));
        }
        if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new IllegalArgumentException(campoInvalido(AtributosAvaliacao.NOTA));
        }
        if (urgencia == null) {
            throw new IllegalArgumentException(campoInvalido(AtributosAvaliacao.URGENCIA));
        }
    }

    /**
     * Monta uma avaliação nova, calculando urgência e datas.
     *
     * @param descricao texto já validado
     * @param nota nota já validada
     * @param id identificador gerado por quem chama
     * @param instante instante do registro, capturado uma vez por requisição
     * @return avaliação pronta para gravar
     */
    public static Avaliacao nova(String descricao, int nota, String id, Instant instante) {
        return new Avaliacao(
                id,
                descricao,
                nota,
                Urgencia.deNota(nota),
                CarimboDeTempo.dataEnvio(instante),
                CarimboDeTempo.criadoEm(instante));
    }

    /**
     * Reconstrói uma avaliação a partir dos atributos do item, já convertidos para texto. Cada
     * função leitora usa um tipo diferente de {@code AttributeValue} e converte antes de chamar.
     *
     * @param atributos atributos do item, pelos nomes de {@link AtributosAvaliacao}
     * @return avaliação correspondente
     * @throws ItemInvalidoException com os nomes dos campos faltantes ou inválidos
     */
    public static Avaliacao deAtributos(Map<String, String> atributos) {
        Objects.requireNonNull(atributos, "atributos");
        List<String> invalidos = new ArrayList<>();

        String id = lerTexto(atributos, AtributosAvaliacao.ID, invalidos);
        String descricao = atributos.get(AtributosAvaliacao.DESCRICAO);
        if (descricao == null || !ValidadorAvaliacao.descricaoValida(descricao)) {
            invalidos.add(AtributosAvaliacao.DESCRICAO);
        }
        Integer nota = lerNota(atributos.get(AtributosAvaliacao.NOTA));
        if (nota == null) {
            invalidos.add(AtributosAvaliacao.NOTA);
        }
        Urgencia urgencia = lerUrgencia(atributos.get(AtributosAvaliacao.URGENCIA));
        if (urgencia == null) {
            invalidos.add(AtributosAvaliacao.URGENCIA);
        }
        String dataEnvio = lerTexto(atributos, AtributosAvaliacao.DATA_ENVIO, invalidos);
        String criadoEm = lerTexto(atributos, AtributosAvaliacao.CRIADO_EM, invalidos);

        if (!invalidos.isEmpty()) {
            throw new ItemInvalidoException(invalidos);
        }
        return new Avaliacao(id, descricao, nota, urgencia, dataEnvio, criadoEm);
    }

    // O toString gerado pelo record exporia descricao e nota a quem registrasse o objeto em log.
    @Override
    public String toString() {
        return "Avaliacao[id=" + id
                + ", urgencia=" + urgencia
                + ", dataEnvio=" + dataEnvio
                + ", criadoEm=" + criadoEm + "]";
    }

    private static String lerTexto(Map<String, String> atributos, String nome, List<String> invalidos) {
        String valor = atributos.get(nome);
        if (valor == null || valor.isEmpty()) {
            invalidos.add(nome);
        }
        return valor;
    }

    // Sem encadear a NumberFormatException: a mensagem dela cita o valor lido.
    private static Integer lerNota(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            int nota = Integer.parseInt(texto);
            return nota >= NOTA_MINIMA && nota <= NOTA_MAXIMA ? nota : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Urgencia lerUrgencia(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Urgencia.valueOf(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void exigirTexto(String valor, String nome) {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException(campoInvalido(nome));
        }
    }

    private static String campoInvalido(String nome) {
        return "Campo inválido: " + nome;
    }
}
