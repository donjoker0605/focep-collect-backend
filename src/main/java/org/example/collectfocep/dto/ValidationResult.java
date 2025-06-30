package org.example.collectfocep.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean success;
    private String errorCode;
    private String message;

    public static ValidationResult success() {
        return ValidationResult.builder()
                .success(true)
                .build();
    }

    public static ValidationResult failure(String errorCode, String message) {
        return ValidationResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}