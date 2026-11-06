package br.com.fiap.feedback.relatorio;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Roda o priming que o Lambda executa ao publicar a versão com SnapStart.
 *
 * <p>Sem {@code aspectjrt} no classpath, o preload lança {@code NoClassDefFoundError}. Por ser
 * {@code Error}, ele escapa de {@code loadClassIfFound} e derruba a inicialização, e sem este teste
 * só apareceria no deploy.
 */
class PrimingSnapStartTest {

    @Test
    @DisplayName("O preload das listas de classes do Powertools não lança nada")
    void preloadDeClasses() {
        assertThatCode(ClassPreLoader::preloadClasses).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("O hook beforeCheckpoint de TracingUtils não lança nada")
    void hookDeTracingUtils() {
        assertThatCode(() -> new TracingUtils().beforeCheckpoint(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("O hook beforeCheckpoint de MetricsFactory não lança nada")
    void hookDeMetricsFactory() {
        assertThatCode(() -> new MetricsFactory().beforeCheckpoint(null)).doesNotThrowAnyException();
    }
}
