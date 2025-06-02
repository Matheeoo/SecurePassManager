package com.securepassmanager;

import com.securepassmanager.model.PasswordEntry;
import com.securepassmanager.security.EncryptionService;
import com.securepassmanager.security.TwoFactorAuth;
import com.securepassmanager.security.PasswordBreachChecker;
import com.securepassmanager.service.MongoDBService;
import com.securepassmanager.model.User;
import com.securepassmanager.service.UserService;

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
    private static MongoDBService mongoDBService;
    private static UserService userService;
    private static Scanner scanner;
    private static String masterPassword;
    private static User loggedUser;

    public static void main(String[] args) {
        try {
            initializeServices();
            scanner = new Scanner(System.in);

            // Fluxo de cadastro/login de usuário
            while (loggedUser == null) {
                System.out.println("\n=== SecurePassManager ===");
                System.out.println("1. Login");
                System.out.println("2. Cadastrar novo usuário");
                System.out.println("3. Sair");
                int op = getIntInput("Escolha uma opção: ");
                switch (op) {
                    case 1:
                        loginUser();
                        break;
                    case 2:
                        registerUser();
                        break;
                    case 3:
                        System.out.println("Encerrando programa...");
                        return;
                    default:
                        System.out.println("Opção inválida!");
                }
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
            if (mongoDBService != null) {
                mongoDBService.close();
            }
            if (userService != null) {
                userService.close();
            }
        }
    }

    private static void initializeServices() throws Exception {
        encryptionService = new EncryptionService();
        twoFactorAuth = null; // Será inicializado após login
        breachChecker = new PasswordBreachChecker();
        mongoDBService = new MongoDBService();
        userService = new UserService();
    }

    private static void registerUser() {
        System.out.println("\n=== Cadastro de Usuário ===");
        String email = getStringInput("Email: ");
        String password = getPasswordInput("Senha mestra: ");
        String confirmPassword = getPasswordInput("Confirme a senha mestra: ");
        if (!password.equals(confirmPassword)) {
            System.out.println("As senhas não coincidem!");
            return;
        }
        if (userService.findByEmail(email) != null) {
            System.out.println("Já existe um usuário com esse email!");
            return;
        }
        // Gera segredo TOTP
        TwoFactorAuth temp2fa = new TwoFactorAuth();
        String totpSecret = temp2fa.getSecret();
        String passwordHash = encryptionService.hashPassword(password);
        User user = new User(email, passwordHash, totpSecret);
        userService.registerUser(user);
        System.out.println("Usuário cadastrado com sucesso!");
        System.out.println("Configure o 2FA escaneando o QR Code acima no seu app autenticador.");
    }

    private static void loginUser() {
        System.out.println("\n=== Login ===");
        String email = getStringInput("Email: ");
        String password = getPasswordInput("Senha mestra: ");
        User user = userService.findByEmail(email);
        if (user == null) {
            System.out.println("Usuário não encontrado!");
            return;
        }
        if (!encryptionService.verifyPassword(password, user.getPasswordHash())) {
            System.out.println("Senha incorreta!");
            return;
        }
        // Inicializa 2FA com segredo do usuário
        twoFactorAuth = new TwoFactorAuth(user.getTotpSecret());
        if (!twoFactorAuth.verifyCode()) {
            System.out.println("Código 2FA inválido!");
            return;
        }
        loggedUser = user;
        masterPassword = password;
        System.out.println("Login realizado com sucesso!");
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
        PasswordEntry entry = new PasswordEntry(service, username, encryptedPassword, loggedUser.getId());
        mongoDBService.insertPasswordEntry(entry);

        System.out.println("Senha registrada com sucesso!");
    }

    private static void retrievePassword() throws Exception {
        String service = getStringInput("Nome do serviço: ");
        PasswordEntry entry = mongoDBService.findByService(service, loggedUser.getId());

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