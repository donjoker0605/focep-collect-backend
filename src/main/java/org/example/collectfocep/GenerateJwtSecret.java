package org.example.collectfocep;

import java.util.Base64;
import java.security.SecureRandom;

public class GenerateJwtSecret {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[64]; // 512 bits
        random.nextBytes(keyBytes);
        String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println(encodedKey);
    }
}
