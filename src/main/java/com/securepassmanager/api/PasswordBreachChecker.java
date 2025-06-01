package com.securepassmanager.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe responsável por verificar se uma senha foi vazada usando a API HaveIBeenPwned.
 * Implementa o protocolo k-anonymity para proteger a privacidade das senhas.
 */
public class PasswordBreachChecker {
    private static final String API_URL = "https://api.pwnedpasswords.com/range/";
    private final OkHttpClient client;

    public PasswordBreachChecker() {
        this.client = new OkHttpClient();
    }

    /**
     * Verifica se uma senha foi vazada.
     * @param password A senha a ser verificada
     * @return O número de vezes que a senha foi vazada, ou 0 se não foi encontrada
     */
    public int checkPassword(String password) throws Exception {
        String sha1Hash = getSHA1Hash(password);
        String prefix = sha1Hash.substring(0, 5);
        String suffix = sha1Hash.substring(5).toUpperCase();

        Request request = new Request.Builder()
                .url(API_URL + prefix)
                .addHeader("User-Agent", "SecurePassManager")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Erro ao verificar a senha: " + response.code());
            }

            String responseBody = response.body().string();
            return findHashInResponse(responseBody, suffix);
        }
    }

    /**
     * Gera o hash SHA-1 de uma senha.
     */
    private String getSHA1Hash(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Converte um array de bytes para uma string hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Procura o hash na resposta da API e retorna o número de vazamentos.
     */
    private int findHashInResponse(String response, String hashSuffix) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts[0].equals(hashSuffix)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }
} 