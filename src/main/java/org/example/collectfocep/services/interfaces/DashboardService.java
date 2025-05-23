package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.DashboardDTO;
import org.example.collectfocep.entities.Collecteur;

public interface DashboardService {
    DashboardDTO buildDashboard(Collecteur collecteur);
}