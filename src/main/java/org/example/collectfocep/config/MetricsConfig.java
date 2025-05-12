package org.example.collectfocep.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MetricsConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public Counter epargneCounter(MeterRegistry registry) {
        return Counter.builder("epargne.operations")
                .description("Nombre d'opérations d'épargne effectuées")
                .register(registry);
    }

    @Bean
    public Counter retraitCounter(MeterRegistry registry) {
        return Counter.builder("collecte.retrait.count")
                .description("Nombre d'opérations de retrait effectuées")
                .register(registry);
    }

    @Bean
    public Counter versementCounter(MeterRegistry registry) {
        return Counter.builder("collecte.versement.count")
                .description("Nombre d'opérations de versement effectuées")
                .register(registry);
    }

    @Bean
    public Counter commissionCounter(MeterRegistry registry) {
        return Counter.builder("collecte.commission.count")
                .description("Nombre de calculs de commission effectués")
                .register(registry);
    }

    @Bean
    public Timer mouvementTimer(MeterRegistry registry) {
        return Timer.builder("mouvement.duration")
                .description("Durée d'exécution des mouvements")
                .register(registry);
    }

    @Bean
    public Timer commissionTimer(MeterRegistry registry) {
        return Timer.builder("collecte.commission.duration")
                .description("Durée des calculs de commission")
                .register(registry);
    }

    @Bean
    public Timer epargneTimer(MeterRegistry registry) {
        return Timer.builder("collecte.epargne.duration")
                .description("Durée des opérations d'épargne")
                .register(registry);
    }

    @Bean
    public Timer retraitTimer(MeterRegistry registry) {
        return Timer.builder("collecte.retrait.duration")
                .description("Durée des opérations de retrait")
                .register(registry);
    }
}