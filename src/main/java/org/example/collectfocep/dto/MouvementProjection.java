package org.example.collectfocep.dto;

import java.time.LocalDateTime;

public interface MouvementProjection {
    Long getId();
    Double getMontant();
    String getLibelle();
    String getSens();
    LocalDateTime getDateOperation();
    String getCompteSourceNumero();
    String getCompteDestinationNumero();
}