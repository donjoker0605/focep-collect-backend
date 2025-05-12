package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferEventDTO {
    private LocalDateTime timestamp;
    private String action;
    private String username;
    private String details;
}