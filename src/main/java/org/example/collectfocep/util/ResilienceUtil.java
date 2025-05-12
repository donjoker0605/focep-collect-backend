package org.example.collectfocep.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceUtil {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Exécute une fonction avec protection par circuit breaker
     *
     * @param supplier La fonction à exécuter
     * @param circuitBreakerName Le nom du circuit breaker à utiliser
     * @return Le résultat de la fonction
     * @param <T> Le type de retour
     */
    public <T> T executeWithCircuitBreaker(Supplier<T> supplier, String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        return Try.ofSupplier(decoratedSupplier).get();
    }

    /**
     * Exécute une fonction avec protection par circuit breaker et un fallback
     *
     * @param supplier La fonction à exécuter
     * @param fallback La fonction de fallback à exécuter en cas d'erreur
     * @param circuitBreakerName Le nom du circuit breaker à utiliser
     * @return Le résultat de la fonction ou du fallback
     * @param <T> Le type de retour
     */
    public <T> T executeWithCircuitBreakerAndFallback(Supplier<T> supplier, Function<Throwable, T> fallback, String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        return Try.ofSupplier(decoratedSupplier)
                .recover(fallback)
                .get();
    }

    /**
     * Exécute une fonction avec protection par retry
     *
     * @param supplier La fonction à exécuter
     * @param retryName Le nom du retry à utiliser
     * @return Le résultat de la fonction
     * @param <T> Le type de retour
     */
    public <T> T executeWithRetry(Supplier<T> supplier, String retryName) {
        Retry retry = retryRegistry.retry(retryName);
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        return Try.ofSupplier(decoratedSupplier).get();
    }

    /**
     * Exécute une fonction avec protection par retry et un fallback
     *
     * @param supplier La fonction à exécuter
     * @param fallback La fonction de fallback à exécuter en cas d'erreur
     * @param retryName Le nom du retry à utiliser
     * @return Le résultat de la fonction ou du fallback
     * @param <T> Le type de retour
     */
    public <T> T executeWithRetryAndFallback(Supplier<T> supplier, Function<Throwable, T> fallback, String retryName) {
        Retry retry = retryRegistry.retry(retryName);
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        return Try.ofSupplier(decoratedSupplier)
                .recover(fallback)
                .get();
    }

    /**
     * Exécute une fonction avec protection par circuit breaker et retry
     *
     * @param supplier La fonction à exécuter
     * @param circuitBreakerName Le nom du circuit breaker à utiliser
     * @param retryName Le nom du retry à utiliser
     * @return Le résultat de la fonction
     * @param <T> Le type de retour
     */
    public <T> T executeWithCircuitBreakerAndRetry(Supplier<T> supplier, String circuitBreakerName, String retryName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        Retry retry = retryRegistry.retry(retryName);

        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);

        return Try.ofSupplier(decoratedSupplier).get();
    }

    /**
     * Exécute une fonction avec protection par circuit breaker, retry et un fallback
     *
     * @param supplier La fonction à exécuter
     * @param fallback La fonction de fallback à exécuter en cas d'erreur
     * @param circuitBreakerName Le nom du circuit breaker à utiliser
     * @param retryName Le nom du retry à utiliser
     * @return Le résultat de la fonction ou du fallback
     * @param <T> Le type de retour
     */
    public <T> T executeWithCircuitBreakerRetryAndFallback(
            Supplier<T> supplier,
            Function<Throwable, T> fallback,
            String circuitBreakerName,
            String retryName) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        Retry retry = retryRegistry.retry(retryName);

        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);

        return Try.ofSupplier(decoratedSupplier)
                .recover(fallback)
                .get();
    }
}