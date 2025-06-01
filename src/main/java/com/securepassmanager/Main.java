package com.securepassmanager;

import com.securepassmanager.model.PasswordEntry;
import com.securepassmanager.security.EncryptionService;
import com.securepassmanager.security.TwoFactorAuth;
import com.securepassmanager.security.PasswordBreachChecker;
import com.securepassmanager.storage.PasswordStorage;

import java.util.Scanner;

/**
 * Classe principal do aplicativo SecurePassManager.
 * Implementa a interface de linha de comando para interação com o usuário.
 */
public class Main {
    private static final String EMAIL = "seu.email@gmail.com"; // Configure seu email aqui
    private static final String EMAIL_PASSWORD = "sua_senha_de_app"; // Configure sua senha de app aqui
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private static EncryptionService encryptionService;
    private static TwoFactorAuth twoFactorAuth;
    private static PasswordBreachChecker breachChecker;
    private static PasswordStorage storage;
    private static Scanner scanner;
    private static String masterPassword;

    public static void main(String[] args) {
        try {
            initializeServices();
            scanner = new Scanner(System.in);

            if (!handleMasterPassword()) {
                System.out.println("Acesso negado. Encerrando programa.");
                return;
            }

            while (true) {
                showMenu();
                int choice = getIntInput("Escolha uma opção: ");

                switch (choice) {
                    case 1:
                        registerNewPassword();
                        break;
                    case 2:
                        retrievePassword();
                        break;
                    case 3:
                        generateStrongPassword();
                        break;
                    case 4:
                        checkPasswordBreach();
                        break;
                    case 5:
                        System.out.println("Encerrando programa...");
                        return;
                    default:
                        System.out.println("Opção inválida!");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static void initializeServices() throws Exception {
        encryptionService = new EncryptionService();
        twoFactorAuth = new TwoFactorAuth(EMAIL, EMAIL_PASSWORD, SMTP_HOST, SMTP_PORT);
        breachChecker = new PasswordBreachChecker();
        storage = new PasswordStorage();
    }

    private static boolean handleMasterPassword() {
        String storedMasterPassword = storage.loadMasterPassword();
        
        if (storedMasterPassword == null) {
            System.out.println("Primeiro acesso! Vamos criar sua senha mestra.");
            String newMasterPassword = getPasswordInput("Digite sua nova senha mestra: ");
            String confirmPassword = getPasswordInput("Confirme sua senha mestra: ");
            
            if (!newMasterPassword.equals(confirmPassword)) {
                System.out.println("As senhas não coincidem!");
                return false;
            }
            
            masterPassword = newMasterPassword;
            storage.saveMasterPassword(encryptionService.hashPassword(newMasterPassword));
            System.out.println("Senha mestra criada com sucesso!");
            return true;
        } else {
            System.out.println("Bem-vindo de volta! Por favor, faça login.");
            String inputPassword = getPasswordInput("Digite sua senha mestra: ");
            
            if (!encryptionService.verifyPassword(inputPassword, storedMasterPassword)) {
                System.out.println("Senha mestra incorreta!");
                return false;
            }
            
            masterPassword = inputPassword;
            return true;
        }
    }

    private static void showMenu() {
        System.out.println("\n=== SecurePassManager ===");
        System.out.println("1. Registrar nova senha");
        System.out.println("2. Recuperar senha");
        System.out.println("3. Gerar senha forte");
        System.out.println("4. Verificar vazamento de senha");
        System.out.println("5. Sair");
    }

    private static void registerNewPassword() throws Exception {
        String service = getStringInput("Nome do serviço: ");
        String username = getStringInput("Nome de usuário: ");
        String password = getPasswordInput("Senha: ");

        if (breachChecker.isPasswordBreached(password)) {
            System.out.println("ATENÇÃO: Esta senha já foi vazada em algum vazamento de dados!");
            if (!getStringInput("Deseja continuar mesmo assim? (s/n): ").equalsIgnoreCase("s")) {
                return;
            }
        }

        String encryptedPassword = encryptionService.encryptPassword(password);
        PasswordEntry entry = new PasswordEntry(service, username, encryptedPassword);
        storage.addPassword(entry);

        System.out.println("Senha registrada com sucesso!");
    }

    private static void retrievePassword() throws Exception {
        String service = getStringInput("Nome do serviço: ");
        PasswordEntry entry = storage.findPassword(service);

        if (entry == null) {
            System.out.println("Serviço não encontrado!");
            return;
        }

        if (getStringInput("Deseja usar autenticação de dois fatores? (s/n): ").equalsIgnoreCase("s")) {
            if (!twoFactorAuth.verifyCode()) {
                System.out.println("Código de verificação inválido!");
                return;
            }
        }

        String decryptedPassword = encryptionService.decryptPassword(entry.getPassword());
        System.out.println("\nDetalhes da senha:");
        System.out.println("Serviço: " + entry.getService());
        System.out.println("Usuário: " + entry.getUsername());
        System.out.println("Senha: " + decryptedPassword);
    }

    private static void generateStrongPassword() {
        int length = getIntInput("Tamanho da senha (mínimo 8): ");
        String password = encryptionService.generateStrongPassword(length);
        System.out.println("\nSenha gerada: " + password);
    }

    private static void checkPasswordBreach() {
        String password = getPasswordInput("Digite a senha para verificar: ");
        if (breachChecker.isPasswordBreached(password)) {
            System.out.println("ATENÇÃO: Esta senha já foi vazada em algum vazamento de dados!");
        } else {
            System.out.println("Senha não encontrada em vazamentos conhecidos.");
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static String getPasswordInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido.");
            }
        }
    }
} 