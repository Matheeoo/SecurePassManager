package com.securepassmanager.security;

import org.mindrot.jbcrypt.BCrypt;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Serviço responsável pela criptografia e descriptografia de senhas.
 * Implementa tanto criptografia simétrica (AES) quanto hash (bcrypt).
 */
public class EncryptionService {
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int BCRYPT_ROUNDS = 12;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutos

    private final SecretKey secretKey;
    private int loginAttempts = 0;
    private long lastFailedAttempt = 0;

    public EncryptionService() throws Exception {
        this.secretKey = KeyManager.getOrCreateKey();
    }

    /**
     * Criptografa uma senha usando AES.
     */
    public String encryptPassword(String password) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Descriptografa uma senha usando AES.
     */
    public String decryptPassword(String encryptedPassword) throws Exception {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("Senha criptografada não pode ser nula ou vazia");
        }

        byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(decoded, 0, iv, 0, iv.length);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        byte[] decryptedBytes = cipher.doFinal(decoded, iv.length, decoded.length - iv.length);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Gera um hash bcrypt para uma senha.
     */
    public String hashPassword(String password) {
        validatePasswordStrength(password);
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifica se uma senha corresponde a um hash bcrypt.
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        if (isAccountLocked()) {
            throw new SecurityException("Conta bloqueada temporariamente. Tente novamente mais tarde.");
        }

        boolean isValid = BCrypt.checkpw(password, hashedPassword);
        if (!isValid) {
            loginAttempts++;
            lastFailedAttempt = System.currentTimeMillis();
            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                throw new SecurityException("Muitas tentativas falhas. Conta bloqueada temporariamente.");
            }
        } else {
            loginAttempts = 0;
        }
        return isValid;
    }

    private boolean isAccountLocked() {
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            long timeSinceLastAttempt = System.currentTimeMillis() - lastFailedAttempt;
            if (timeSinceLastAttempt < LOCKOUT_DURATION) {
                return true;
            }
            loginAttempts = 0;
        }
        return false;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasNumber = true;
            else hasSpecial = true;
        }

        if (!(hasUpper && hasLower && hasNumber && hasSpecial)) {
            throw new IllegalArgumentException(
                "A senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais"
            );
        }
    }

    /**
     * Gera uma senha forte aleatória.
     */
    public String generateStrongPassword(int length) {
        if (length < MIN_PASSWORD_LENGTH) {
            length = MIN_PASSWORD_LENGTH;
        }

        String upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerChars = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String allChars = upperChars + lowerChars + numbers + specialChars;

        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        // Garante pelo menos um caractere de cada tipo
        password.append(upperChars.charAt(random.nextInt(upperChars.length())));
        password.append(lowerChars.charAt(random.nextInt(lowerChars.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Preenche o resto da senha
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Embaralha a senha
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }
} 