package com.securepassmanager.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

import java.util.Scanner;

/**
 * Implementação da autenticação de dois fatores (2FA) usando Google Authenticator (TOTP).
 */
public class TwoFactorAuth {
    private final GoogleAuthenticator gAuth;
    private final String secret;

    public TwoFactorAuth() {
        this.gAuth = new GoogleAuthenticator();
        // Gera uma nova chave secreta para o usuário (ideal: salvar/recuperar do banco)
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        this.secret = key.getKey();
        System.out.println("Escaneie este QR Code no seu app autenticador:");
        String qr = GoogleAuthenticatorQRGenerator.getOtpAuthURL("SecurePassManager", "usuario", key);
        System.out.println(qr);
    }

    public TwoFactorAuth(String secret) {
        this.gAuth = new GoogleAuthenticator();
        this.secret = secret;
    }

    public boolean verifyCode() {
        System.out.print("Digite o código do seu app autenticador: ");
        String inputCode = new Scanner(System.in).nextLine();
        try {
            int code = Integer.parseInt(inputCode);
            return gAuth.authorize(secret, code);
        } catch (NumberFormatException e) {
            System.out.println("Código inválido!");
            return false;
        }
    }

    public String getSecret() {
        return secret;
    }
} 