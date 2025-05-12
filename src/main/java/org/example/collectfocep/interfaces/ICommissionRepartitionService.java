package org.example.collectfocep.interfaces;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.dto.RemunerationResult;

import java.time.LocalDate;

public interface ICommissionRepartitionService {
    void repartirCommissions(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin);
    RemunerationResult calculerRemuneration(Collecteur collecteur, double totalCommissions);
    void traiterManquant(Collecteur collecteur, double montantManquant);
}
