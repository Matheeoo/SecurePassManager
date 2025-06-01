package com.securepassmanager.security;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;

/**
 * Implementação da autenticação de dois fatores (2FA) usando Google Authenticator e email.
 */
public class TwoFactorAuth {
    private final String email;
    private final String password;
    private final String smtpHost;
    private final int smtpPort;

    public TwoFactorAuth(String email, String password, String smtpHost, int smtpPort) {
        this.email = email;
        this.password = password;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
    }

    public boolean verifyCode() {
        try {
            // Gera um código de 6 dígitos
            String code = generateCode();
            
            // Envia o código por email
            sendVerificationEmail(code);
            
            // Solicita o código ao usuário
            System.out.print("Digite o código de verificação enviado para seu email: ");
            String inputCode = new java.util.Scanner(System.in).nextLine();
            
            return code.equals(inputCode);
        } catch (Exception e) {
            System.err.println("Erro na autenticação de dois fatores: " + e.getMessage());
            return false;
        }
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void sendVerificationEmail(String code) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(email));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Código de Verificação - SecurePassManager");
        message.setText("Seu código de verificação é: " + code);

        Transport.send(message);
    }
} 