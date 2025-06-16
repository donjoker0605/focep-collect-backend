package org.example.collectfocep.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private Map<String, Object> meta;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.meta = new HashMap<>();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = "Opération réussie";
        return response;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = success(data);
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }


    /**
     * Méthode pour ajouter des métadonnées
     */
    public void addMeta(String key, Object value) {
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }
        this.meta.put(key, value);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.addMeta("errorCode", code);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Getter pour les métadonnées
     */
    public Map<String, Object> getMeta() {
        return meta;
    }

    /**
     * Setter pour les métadonnées
     */
    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}