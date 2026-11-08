package br.com.fiap.feedback.relatorio;

/**
 * Falha que encerra a execução. A mensagem é fixa e não encadeia a causa nem guarda a pilha, porque o
 * runtime do Lambda escreve no log e na resposta da invocação o que recebe.
 */
final class GeracaoRelatorioException extends RuntimeException {

    GeracaoRelatorioException() {
        super("Falha ao gerar o relatório semanal. Consulte os logs da função.", null, false, false);
    }
}
