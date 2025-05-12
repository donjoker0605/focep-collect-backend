package org.example.collectfocep.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final MeterRegistry meterRegistry;

    /**
     * Démarre un chronomètre pour mesurer la durée d'une opération
     * @return Un échantillon de Timer qui peut être arrêté plus tard
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Obtient ou crée un Timer avec le nom spécifié
     * @param name Nom du timer
     * @param tags Tags optionnels pour le timer
     * @return Le Timer créé ou obtenu
     */
    public Timer getTimer(String name, String... tags) {
        return Timer.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }

    /**
     * Incrémente un compteur
     * @param name Nom du compteur
     * @param tags Tags optionnels pour le compteur
     */
    public void incrementCounter(String name, String... tags) {
        meterRegistry.counter(name, tags).increment();
    }

    /**
     * Enregistre une valeur de jauge
     * @param name Nom de la jauge
     * @param value Valeur à enregistrer
     * @param tags Tags optionnels pour la jauge
     */
    public void recordValue(String name, double value, String... tags) {
        meterRegistry.gauge(name, value);
    }
}