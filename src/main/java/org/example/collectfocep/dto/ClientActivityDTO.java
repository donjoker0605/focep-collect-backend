package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientActivityDTO {
    private Long clientId;
    private String nomClient;
    private Integer nombreTransactions;
    private Double volumeTotal;
    private LocalDateTime derniereActivite;
}
