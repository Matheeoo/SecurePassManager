package com.securepassmanager.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.regex.Pattern;

public class PasswordBreachChecker {
    private static final String API_URL = "https://api.pwnedpasswords.com/range/";
    private final HttpClient httpClient;

    public PasswordBreachChecker() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean isPasswordBreached(String password) {
        try {
            String sha1Hash = getSHA1Hash(password);
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5).toUpperCase();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + prefix))
                .header("User-Agent", "SecurePassManager")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                System.err.println("Erro na API: " + response.statusCode());
                return false;
            }

            String[] hashes = response.body().split("\r\n");
            Pattern pattern = Pattern.compile("^" + suffix + ":(\\d+)$");

            for (String hash : hashes) {
                if (pattern.matcher(hash).matches()) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Erro ao verificar vazamento de senha: " + e.getMessage());
            return false;
        }
    }

    private String getSHA1Hash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString().toUpperCase();
    }
} 