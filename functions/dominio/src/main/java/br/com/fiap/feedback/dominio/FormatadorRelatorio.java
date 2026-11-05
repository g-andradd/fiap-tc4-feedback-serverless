package br.com.fiap.feedback.dominio;

import java.util.Map;

/**
 * Escreve o relatório em texto puro, para leitura direta no console do S3. A nota individual não
 * aparece: ela só alimenta a média.
 */
public final class FormatadorRelatorio {

    private FormatadorRelatorio() {
    }

    /**
     * Formata o relatório completo.
     *
     * @param relatorio relatório consolidado
     * @return texto do objeto a gravar
     */
    public static String formatar(RelatorioSemanal relatorio) {
        StringBuilder texto = new StringBuilder("RELATÓRIO SEMANAL DE AVALIAÇÕES\n");
        texto.append(numeros(relatorio.resumo()));
        texto.append("\nAvaliações (").append(relatorio.avaliacoes().size()).append(")\n");
        for (Avaliacao avaliacao : relatorio.avaliacoes()) {
            texto.append("\nData de envio: ").append(avaliacao.dataEnvio())
                    .append(" | Urgência: ").append(avaliacao.urgencia().name())
                    .append("\nDescrição: ").append(avaliacao.descricao())
                    .append('\n');
        }
        return texto.toString();
    }

    /**
     * Bloco comum ao relatório e ao aviso. Recebe só o resumo, sem acesso à lista de avaliações.
     *
     * @param resumo números da semana
     * @return linhas do ambiente até a quantidade por dia, com quebra de linha no fim
     */
    static String numeros(ResumoSemanal resumo) {
        StringBuilder texto = new StringBuilder();
        texto.append("Ambiente: ").append(resumo.ambiente()).append('\n');
        texto.append("Data de referência: ").append(resumo.dataReferencia()).append('\n');
        texto.append("Janela: ").append(resumo.janela().inicio())
                .append(" a ").append(resumo.janela().fim()).append('\n');
        texto.append("\nMédia das notas: ").append(resumo.mediaFormatada()).append('\n');
        texto.append("\nQuantidade por urgência\n");
        escreverTotais(texto, resumo.totaisPorUrgencia());
        texto.append("\nQuantidade por dia\n");
        escreverTotais(texto, resumo.totaisPorDia());
        return texto.toString();
    }

    private static void escreverTotais(StringBuilder texto, Map<?, Integer> totais) {
        totais.forEach((chave, quantidade) ->
                texto.append(chave).append(": ").append(quantidade).append('\n'));
    }
}
