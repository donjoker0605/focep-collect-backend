package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean success;
    private String errorCode;
    private String message;
    private Object data;

    public static ValidationResult success() {
        return ValidationResult.builder()
                .success(true)
                .build();
    }

    public static ValidationResult success(String message) {
        return ValidationResult.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static ValidationResult success(String message, Object data) {
        return ValidationResult.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static ValidationResult error(String errorCode, String message) {
        return ValidationResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static ValidationResult error(String message) {
        return ValidationResult.builder()
                .success(false)
                .message(message)
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }
}