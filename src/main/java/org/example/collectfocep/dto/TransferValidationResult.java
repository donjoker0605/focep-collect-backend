package org.example.collectfocep.dto;

import lombok.Data;
import org.example.collectfocep.entities.Collecteur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class TransferValidationResult {
    
    // État général de la validation
    private boolean valid;
    private String summary;
    
    // Messages
    private List<String> errors;
    private List<String> warnings;
    
    // Détails des collecteurs
    private Collecteur sourceCollecteur;
    private Collecteur targetCollecteur;
    private boolean interAgenceTransfer;
    
    // Détails des clients
    private int totalClientsCount;
    private int validClientsCount;
    private Map<Long, String> clientValidations; // clientId -> statut de validation
    
    // Impact financier
    private BigDecimal totalBalance;
    private BigDecimal estimatedTransferFees;
    private BigDecimal commissionImpact;
    private int clientsWithDebt;
    
    // Règles métier
    private boolean requiresApproval;
    private String approvalReason;
    
    // Méthodes utilitaires
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public int getInvalidClientsCount() {
        return totalClientsCount - validClientsCount;
    }
    
    public double getValidationSuccessRate() {
        if (totalClientsCount == 0) return 0.0;
        return (double) validClientsCount / totalClientsCount * 100.0;
    }
}