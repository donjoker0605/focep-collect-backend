package org.example.collectfocep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * ✅ CONFIGURATION THREAD POOL POUR LES RAPPORTS ASYNCHRONES
     */
    @Bean(name = "reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configuration du pool de threads
        executor.setCorePoolSize(2);           // Nombre minimum de threads
        executor.setMaxPoolSize(5);            // Nombre maximum de threads
        executor.setQueueCapacity(100);        // Taille de la queue
        executor.setThreadNamePrefix("AsyncReport-");
        executor.setKeepAliveSeconds(60);      // Durée de vie des threads inactifs

        // Politique de rejet quand la queue est pleine
        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("⚠️ Génération de rapport rejetée - Queue pleine. Runnable: {}", runnable);
            throw new java.util.concurrent.RejectedExecutionException(
                    "Trop de rapports en cours de génération. Veuillez réessayer plus tard.");
        });

        // Attendre la fin des tâches lors de l'arrêt
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("✅ Thread pool pour rapports configuré: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * ✅ CONFIGURATION THREAD POOL POUR LES TÂCHES GÉNÉRALES
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("AsyncTask-");
        executor.setKeepAliveSeconds(30);

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("⚠️ Tâche asynchrone rejetée - Queue pleine");
            // Ne pas lancer d'exception pour les tâches générales
        });

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}