package com.securepassmanager.security;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Implementação da autenticação de dois fatores (2FA) usando Google Authenticator (TOTP).
 */
public class TwoFactorAuth {
    private static final int BACKUP_CODES_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 10;
    private static final int VERIFICATION_WINDOW = 1;
    private static final int MAX_USED_CODES = 100; // Mantém os últimos 100 códigos usados
    
    private final String secret;
    private final Set<String> backupCodes;
    private final CodeGenerator codeGenerator;
    private final TimeProvider timeProvider;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final Set<String> usedCodes; // Armazena códigos já utilizados

    public TwoFactorAuth() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        this.secret = secretGenerator.generate();
        this.backupCodes = generateBackupCodes();
        this.codeGenerator = new DefaultCodeGenerator();
        this.timeProvider = new SystemTimeProvider();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        this.usedCodes = ConcurrentHashMap.newKeySet();
    }

    public TwoFactorAuth(String secret) {
        this.secret = secret;
        this.backupCodes = new HashSet<>();
        this.codeGenerator = new DefaultCodeGenerator();
        this.timeProvider = new SystemTimeProvider();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        this.usedCodes = ConcurrentHashMap.newKeySet();
    }

    public TwoFactorAuth(String secret, List<String> backupCodesList) {
        this.secret = secret;
        this.backupCodes = new HashSet<>(backupCodesList);
        this.codeGenerator = new DefaultCodeGenerator();
        this.timeProvider = new SystemTimeProvider();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        this.usedCodes = ConcurrentHashMap.newKeySet();
    }

    public String getSecret() {
        return secret;
    }

    public Set<String> getBackupCodes() {
        return backupCodes;
    }

    public String getQrCodeUrl(String email) throws QrGenerationException {
        QrData data = new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer("SecurePassManager")
            .build();
        
        // Retorna a URL otpauth em vez do QR code
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
            "SecurePassManager",
            email,
            secret,
            "SecurePassManager");
    }

    public boolean verifyBackupCode(String code) {
        if (backupCodes.contains(code)) {
            backupCodes.remove(code);
            System.out.println("Código de backup usado. Restam " + backupCodes.size() + " códigos.");
            return true;
        }
        return false;
    }

    public boolean verifyCode() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nDigite o código de 6 dígitos do seu app autenticador");
        System.out.println("Ou digite um código de backup se necessário");
        String code = scanner.nextLine().trim();

        // Verifica se é um código de backup
        if (backupCodes.contains(code)) {
            backupCodes.remove(code);
            System.out.println("Código de backup usado. Restam " + backupCodes.size() + " códigos.");
            return true;
        }

        // Verifica o código TOTP
        try {
            int codeInt = Integer.parseInt(code);
            String formattedCode = String.format("%06d", codeInt);
            
            // Verifica se o código já foi usado
            if (usedCodes.contains(formattedCode)) {
                System.out.println("\n❌ Este código já foi utilizado!");
                return false;
            }

            // Verifica se o código é válido
            if (codeVerifier.isValidCode(secret, formattedCode)) {
                // Adiciona o código à lista de códigos usados
                usedCodes.add(formattedCode);
                
                // Mantém apenas os últimos MAX_USED_CODES códigos
                if (usedCodes.size() > MAX_USED_CODES) {
                    usedCodes.clear(); // Limpa todos os códigos antigos
                }
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Set<String> generateBackupCodes() {
        Set<String> codes = new HashSet<>();
        SecureRandom random = new SecureRandom();
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

        while (codes.size() < BACKUP_CODES_COUNT) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            codes.add(code.toString());
        }

        return codes;
    }

    public void displayBackupCodes() {
        int boxWidth = 32;
        System.out.println();
        printBoxTop(boxWidth);
        printBoxCentered("Códigos de Backup", boxWidth);
        printBoxDivider(boxWidth);
        printBoxCentered("⚠️  Guarde estes códigos!", boxWidth);
        printBoxCentered("Use se perder o 2FA.", boxWidth);
        printBoxDivider(boxWidth);
        for (String code : backupCodes) {
            printBoxCentered(code, boxWidth);
        }
        printBoxBottom(boxWidth);
    }

    private void printBoxTop(int width) {
        System.out.print("╔");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╗");
    }
    private void printBoxBottom(int width) {
        System.out.print("╚");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╝");
    }
    private void printBoxDivider(int width) {
        System.out.print("╠");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╣");
    }
    private void printBoxCentered(String text, int width) {
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        sb.append("║");
        for (int i = 0; i < padding; i++) sb.append(" ");
        sb.append(text);
        while (sb.length() < width + 1) sb.append(" ");
        sb.append("║");
        System.out.println(sb.toString());
    }
} 