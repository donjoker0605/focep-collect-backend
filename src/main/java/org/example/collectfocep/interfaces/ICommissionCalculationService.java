package org.example.collectfocep.interfaces;

import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CommissionParameter;

import java.time.LocalDate;

public interface ICommissionCalculationService {
    CommissionResult calculateCommissionForClient(Client client, LocalDate dateDebut, LocalDate dateFin, double montantTotal);
    void processCollecteurRemuneration(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin);
    CommissionParameter getCommissionParameters(Client client);
}