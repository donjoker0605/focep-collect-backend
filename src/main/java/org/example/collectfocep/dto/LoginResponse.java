package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private String refreshToken; // Optionnel

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }
}