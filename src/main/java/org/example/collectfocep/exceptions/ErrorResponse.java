package org.example.collectfocep.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String code;
    private final String message;
    private final String detail;
    private final LocalDateTime timestamp;
    private final String path;
    private final Map<String, String> validationErrors;
    private final List<String> stackTrace;
}
