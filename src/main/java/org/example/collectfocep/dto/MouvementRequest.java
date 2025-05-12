package org.example.collectfocep.dto;
public class MouvementRequest {
    private Long clientId;
    private double montant;
    private Long journalId;

    // Constructeurs
    public MouvementRequest() {}

    public MouvementRequest(Long clientId, double montant, Long journalId) {
        this.clientId = clientId;
        this.montant = montant;
        this.journalId = journalId;
    }

    // Getters et setters
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public Long getJournalId() {
        return journalId;
    }

    public void setJournalId(Long journalId) {
        this.journalId = journalId;
    }
}