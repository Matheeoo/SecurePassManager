package com.securepassmanager;

import com.securepassmanager.model.PasswordEntry;
import com.securepassmanager.security.EncryptionService;
import com.securepassmanager.security.TwoFactorAuth;
import com.securepassmanager.security.PasswordBreachChecker;
import com.securepassmanager.service.MongoDBService;
import com.securepassmanager.model.User;
import com.securepassmanager.service.UserService;
import com.securepassmanager.service.SyncService;

import java.util.Scanner;
import java.util.regex.Pattern;
import java.text.Normalizer;

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
    private static MongoDBService mongoDBServiceLocal;
    private static MongoDBService mongoDBServiceCloud;
    private static SyncService syncService;
    private static UserService userService;
    private static Scanner scanner;
    private static String masterPassword;
    private static User loggedUser;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️  Encerrando programa...");
            cleanup();
        }));

        try {
            initializeServices();
            scanner = new Scanner(System.in);

            // Fluxo de cadastro/login de usuário
            while (loggedUser == null) {
                System.out.println("\n╔════════════════════════════════════╗");
                System.out.println("║        SecurePassManager           ║");
                System.out.println("╠════════════════════════════════════╣");
                System.out.println("║ 1. Login                           ║");
                System.out.println("║ 2. Cadastrar novo usuário          ║");
                System.out.println("║ 3. Sair                            ║");
                System.out.println("╚════════════════════════════════════╝");
                int op = getIntInput("Escolha uma opção: ");
                switch (op) {
                    case 1:
                        loginUser();
                        break;
                    case 2:
                        registerUser();
                        break;
                    case 3:
                        System.out.println("\n👋 Encerrando programa...");
                        cleanup();
                        System.exit(0);
                        return;
                    default:
                        System.out.println("\n❌ Opção inválida!");
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
                        changeMasterPassword();
                        break;
                    case 6:
                        System.out.println("\n👋 Encerrando programa...");
                        cleanup();
                        System.exit(0);
                        return;
                    default:
                        System.out.println("\n❌ Opção inválida!");
                }
            }
        } catch (Exception e) {
            System.err.println("\n❌ Erro: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static void initializeServices() throws Exception {
        encryptionService = new EncryptionService();
        breachChecker = new PasswordBreachChecker();
        mongoDBService = new MongoDBService();
        userService = new UserService();
        // Inicializa serviços local e nuvem para sincronização
        mongoDBServiceLocal = new MongoDBService() {
            @Override
            public boolean isCloudConnection() { return false; }
        };
        mongoDBServiceCloud = new MongoDBService() {
            @Override
            public boolean isCloudConnection() { return true; }
        };
        syncService = new SyncService(mongoDBServiceLocal, mongoDBServiceCloud);
    }

    private static void registerUser() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Cadastro de Usuário         ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String email = getStringInput("Email: ");
            if (!isValidEmail(email)) {
                System.out.println("\n❌ E-mail inválido!");
                return;
            }
            String password = getPasswordInput("Senha mestra: ");
            String confirmPassword = getPasswordInput("Confirme a senha mestra: ");

            if (!password.equals(confirmPassword)) {
                System.out.println("\n❌ As senhas não coincidem!");
                return;
            }

            if (userService.findByEmail(email) != null) {
                System.out.println("\n❌ Já existe um usuário com esse email!");
                return;
            }

            // Gera segredo TOTP e códigos de backup
            TwoFactorAuth temp2fa = new TwoFactorAuth();
            String totpSecret = temp2fa.getSecret();
            String passwordHash = encryptionService.hashPassword(password);
            
            print2FABox(temp2fa.getQrCodeUrl(email));
            
            // Exibe códigos de backup
            temp2fa.displayBackupCodes();

            System.out.println("\n⚠️  Por favor, digite o código de 6 dígitos do seu app autenticador para confirmar a configuração:");
            if (!temp2fa.verifyCode()) {
                System.out.println("\n❌ Código inválido! Por favor, tente novamente.");
                return;
            }

            User user = new User(email, passwordHash, totpSecret);
            user.setBackupCodes(new java.util.ArrayList<>(temp2fa.getBackupCodes()));
            userService.registerUser(user);
            System.out.println("\n✅ Usuário cadastrado com sucesso!");
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao cadastrar usuário: " + e.getMessage());
        }
    }

    private static void print2FABox(String qrUrl) {
        int boxWidth = 60;
        printBoxTop(boxWidth);
        printBoxCentered("Configuração do 2FA", boxWidth);
        printBoxDivider(boxWidth);
        printBoxWrapped("Para configurar o 2FA, você tem duas opções:", boxWidth);
        printBoxEmpty(boxWidth);
        printBoxWrapped("1. Copie a URL abaixo no app autenticador:", boxWidth);
        printBoxEmpty(boxWidth);
        printBoxWrapped("2. Ou escaneie o QR code pelo link abaixo:", boxWidth);
        printBoxBottom(boxWidth);
        // Exibe as URLs fora da caixa para fácil cópia
        System.out.println("\nCopie a linha abaixo no app autenticador:");
        System.out.println(qrUrl + "\n");
        System.out.println("Ou acesse este link para o QR code:");
        System.out.println("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + java.net.URLEncoder.encode(qrUrl, java.nio.charset.StandardCharsets.UTF_8) + "\n");
    }

    private static void printBoxTop(int width) {
        System.out.print("╔");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╗");
    }

    private static void printBoxBottom(int width) {
        System.out.print("╚");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╝");
    }

    private static void printBoxDivider(int width) {
        System.out.print("╠");
        for (int i = 0; i < width; i++) System.out.print("═");
        System.out.println("╣");
    }

    private static void printBoxCentered(String text, int width) {
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        sb.append("║");
        for (int i = 0; i < padding; i++) sb.append(" ");
        sb.append(text);
        while (sb.length() < width + 1) sb.append(" ");
        sb.append("║");
        System.out.println(sb.toString());
    }

    private static void printBoxWrapped(String text, int width) {
        int maxLen = width - 2;
        for (int i = 0; i < text.length(); i += maxLen) {
            String line = text.substring(i, Math.min(i + maxLen, text.length()));
            StringBuilder sb = new StringBuilder();
            sb.append("║ ");
            sb.append(line);
            while (sb.length() < width + 1) sb.append(" ");
            sb.append("║");
            System.out.println(sb.toString());
        }
    }

    private static void printBoxEmpty(int width) {
        StringBuilder sb = new StringBuilder();
        sb.append("║");
        for (int i = 0; i < width; i++) sb.append(" ");
        sb.append("║");
        System.out.println(sb.toString());
    }

    private static void loginUser() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║              Login                 ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String email = getStringInput("Email: ");
            if (!isValidEmail(email)) {
                System.out.println("\n❌ E-mail inválido!");
                return;
            }
            String password = getPasswordInput("Senha mestra: ");
            
            User user = userService.findByEmail(email);
            if (user == null) {
                System.out.println("\n❌ Usuário não encontrado!");
                return;
            }

            if (!encryptionService.verifyPassword(password, user.getPasswordHash())) {
                System.out.println("\n❌ Senha incorreta!");
                return;
            }

            // Inicializa 2FA com segredo e códigos de backup do usuário
            twoFactorAuth = new TwoFactorAuth(user.getTotpSecret(), user.getBackupCodes());
            
            // Sincronização automática após login
            if (mongoDBService.isCloudConnection()) {
                System.out.println("\n🔄 Sincronizando dados entre local e nuvem...");
                syncService.syncBidirectional(user.getId());
            }

            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║    Autenticação de Dois Fatores    ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║ Escolha o método de autenticação:  ║");
            System.out.println("║ 1. Código do app autenticador      ║");
            System.out.println("║ 2. Código de backup                ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String authInput = getStringInput("Escolha uma opção ou digite seu código de backup: ");
            boolean isValid = false;
            if (authInput.equals("1")) {
                isValid = twoFactorAuth.verifyCode();
            } else if (authInput.equals("2")) {
                System.out.println("\nDigite um dos seus códigos de backup:");
                String backupCode = getStringInput("Código: ");
                isValid = twoFactorAuth.verifyBackupCode(backupCode);
                if (isValid) {
                    user.setBackupCodes(new java.util.ArrayList<>(twoFactorAuth.getBackupCodes()));
                    userService.updateUser(user);
                }
            } else {
                // Tenta validar como código de backup direto
                isValid = twoFactorAuth.verifyBackupCode(authInput);
                if (isValid) {
                    user.setBackupCodes(new java.util.ArrayList<>(twoFactorAuth.getBackupCodes()));
                    userService.updateUser(user);
                } else {
                    System.out.println("\n❌ Opção inválida ou código de backup incorreto!");
                    return;
                }
            }

            if (!isValid) {
                System.out.println("\n❌ Código inválido!");
                return;
            }

            loggedUser = user;
            System.out.println("\n✅ Login realizado com sucesso!");
        } catch (SecurityException e) {
            System.out.println("\n❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao fazer login: " + e.getMessage());
        }
    }

    private static void showMenu() {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║        SecurePassManager           ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║ 1. Registrar nova senha           ║");
        System.out.println("║ 2. Recuperar senha                ║");
        System.out.println("║ 3. Gerar senha forte              ║");
        System.out.println("║ 4. Verificar vazamento de senha   ║");
        System.out.println("║ 5. Alterar senha mestra           ║");
        System.out.println("║ 6. Sair                           ║");
        System.out.println("╚════════════════════════════════════╝");
    }

    private static void registerNewPassword() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Nova Senha                  ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String service = getStringInput("Nome do serviço: ");
            service = sanitizeInput(service);
            if (!isValidServiceOrUsername(service)) {
                System.out.println("\n❌ Nome de serviço inválido! Use apenas letras, números, ponto, traço e sublinhado (2-64 caracteres).");
                return;
            }
            String username = getStringInput("Nome de usuário: ");
            username = sanitizeInput(username);
            if (!isValidServiceOrUsername(username)) {
                System.out.println("\n❌ Nome de usuário inválido! Use apenas letras, números, ponto, traço e sublinhado (2-64 caracteres).");
                return;
            }
            String password = getPasswordInput("Senha: ");

            if (breachChecker.isPasswordBreached(password)) {
                System.out.println("\n⚠️  ATENÇÃO: Esta senha já foi vazada em algum vazamento de dados!");
                if (!getStringInput("Deseja continuar mesmo assim? (s/n): ").equalsIgnoreCase("s")) {
                    return;
                }
            }

            String encryptedPassword = encryptionService.encryptPassword(password);
            PasswordEntry entry = new PasswordEntry(service, username, encryptedPassword, loggedUser.getId());
            mongoDBService.insertPasswordEntry(entry);

            System.out.println("\n✅ Senha registrada com sucesso!");
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao registrar senha: " + e.getMessage());
        }
    }

    private static void retrievePassword() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Recuperar Senha             ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String service = getStringInput("Nome do serviço: ");
            service = sanitizeInput(service);
            if (!isValidServiceOrUsername(service)) {
                System.out.println("\n❌ Nome de serviço inválido! Use apenas letras, números, ponto, traço e sublinhado (2-64 caracteres).");
                return;
            }
            PasswordEntry entry = mongoDBService.findByService(service, loggedUser.getId());

            if (entry == null) {
                System.out.println("\n❌ Serviço não encontrado!");
                return;
            }

            System.out.println("\n⚠️  Autenticação de dois fatores necessária para visualizar a senha.");
            System.out.println("Escolha o método de autenticação:");
            System.out.println("1. Código do app autenticador");
            System.out.println("2. Código de backup");
            String authInput = getStringInput("Escolha uma opção ou digite seu código de backup: ");
            boolean isValid = false;
            if (authInput.equals("1")) {
                isValid = twoFactorAuth.verifyCode();
            } else if (authInput.equals("2")) {
                System.out.println("\nDigite um dos seus códigos de backup:");
                String backupCode = getStringInput("Código: ");
                isValid = twoFactorAuth.verifyBackupCode(backupCode);
            } else {
                // Tenta validar como código de backup direto
                isValid = twoFactorAuth.verifyBackupCode(authInput);
                if (!isValid) {
                    System.out.println("\n❌ Opção inválida ou código de backup incorreto!");
                    return;
                }
            }

            if (!isValid) {
                System.out.println("\n❌ Código inválido!");
                return;
            }

            String decryptedPassword = encryptionService.decryptPassword(entry.getPassword());
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Detalhes da Senha           ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║ Serviço: " + padRight(entry.getService(), 25) + "║");
            System.out.println("║ Usuário: " + padRight(entry.getUsername(), 25) + "║");
            System.out.println("║ Senha:   " + padRight(decryptedPassword, 25) + "║");
            System.out.println("╚════════════════════════════════════╝");
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao recuperar senha: " + e.getMessage());
        }
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static void generateStrongPassword() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Gerar Senha Forte           ║");
            System.out.println("╚════════════════════════════════════╝");
            
            int length = getIntInput("Tamanho da senha (mínimo 12): ");
            String password = encryptionService.generateStrongPassword(length);
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║        Senha Gerada                ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║ " + padRight(password, 32) + "║");
            System.out.println("╚════════════════════════════════════╝");
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao gerar senha: " + e.getMessage());
        }
    }

    private static void checkPasswordBreach() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║    Verificar Vazamento de Senha    ║");
            System.out.println("╚════════════════════════════════════╝");
            
            String password = getPasswordInput("Digite a senha para verificar: ");
            if (breachChecker.isPasswordBreached(password)) {
                System.out.println("\n⚠️  ATENÇÃO: Esta senha já foi vazada em algum vazamento de dados!");
            } else {
                System.out.println("\n✅ Senha não encontrada em vazamentos conhecidos.");
            }
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao verificar senha: " + e.getMessage());
        }
    }

    private static void changeMasterPassword() {
        try {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║      Alterar Senha Mestra          ║");
            System.out.println("╚════════════════════════════════════╝");
            String currentPassword = getPasswordInput("Senha mestra atual: ");
            if (!encryptionService.verifyPassword(currentPassword, loggedUser.getPasswordHash())) {
                System.out.println("\n❌ Senha mestra atual incorreta!");
                return;
            }
            String newPassword = getPasswordInput("Nova senha mestra: ");
            String confirmPassword = getPasswordInput("Confirme a nova senha mestra: ");
            if (!newPassword.equals(confirmPassword)) {
                System.out.println("\n❌ As senhas não coincidem!");
                return;
            }
            // 2FA obrigatório para trocar senha mestra
            System.out.println("\n⚠️  Autenticação de dois fatores obrigatória para alterar a senha mestra.");
            System.out.println("Escolha o método de autenticação:");
            System.out.println("1. Código do app autenticador");
            System.out.println("2. Código de backup");
            String authInput = getStringInput("Escolha uma opção ou digite seu código de backup: ");
            boolean isValid = false;
            if (authInput.equals("1")) {
                isValid = twoFactorAuth.verifyCode();
            } else if (authInput.equals("2")) {
                System.out.println("\nDigite um dos seus códigos de backup:");
                String backupCode = getStringInput("Código: ");
                isValid = twoFactorAuth.verifyBackupCode(backupCode);
            } else {
                isValid = twoFactorAuth.verifyBackupCode(authInput);
                if (!isValid) {
                    System.out.println("\n❌ Opção inválida ou código de backup incorreto!");
                    return;
                }
            }
            if (!isValid) {
                System.out.println("\n❌ Código inválido!");
                return;
            }
            // Atualiza o hash da senha mestra
            String newHash = encryptionService.hashPassword(newPassword);
            loggedUser.setPasswordHash(newHash);
            userService.updateUser(loggedUser);
            System.out.println("\n✅ Senha mestra alterada com sucesso!");
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao alterar senha mestra: " + e.getMessage());
        }
    }

    private static String sanitizeInput(String input) {
        if (input == null) return null;
        // Remove espaços extras e caracteres de controle
        String sanitized = input.trim().replaceAll("[\n\r\t]", "");
        // Remove caracteres potencialmente perigosos
        sanitized = sanitized.replaceAll("[<>;]", "");
        // Remove emojis e caracteres não ASCII
        sanitized = sanitized.replaceAll("[^\\p{ASCII}]", "");
        // Remove acentuação
        sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        // Limita tamanho máximo (ex: 64 caracteres)
        if (sanitized.length() > 64) sanitized = sanitized.substring(0, 64);
        return sanitized;
    }

    private static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }

    private static boolean isValidServiceOrUsername(String value) {
        if (value == null || value.isEmpty()) return false;
        // Apenas letras, números, pontos, traços e sublinhados
        return value.matches("^[A-Za-z0-9._-]{2,64}$");
    }

    private static boolean isValidBackupCode(String code) {
        if (code == null) return false;
        // Exemplo: backup codes de 8 dígitos/letras
        return code.matches("^[A-Za-z0-9]{6,12}$");
    }

    private static String getStringInput(String prompt) {
        try {
            System.out.print(prompt);
            String input = scanner.nextLine();
            return sanitizeInput(input);
        } catch (IllegalStateException e) {
            System.out.println("\n⚠️  Entrada interrompida. Encerrando...");
            cleanup();
            System.exit(0);
            return "";
        }
    }

    private static String getPasswordInput(String prompt) {
        try {
            System.out.print(prompt);
            return scanner.nextLine();
        } catch (IllegalStateException e) {
            System.out.println("\n⚠️  Entrada interrompida. Encerrando...");
            cleanup();
            System.exit(0);
            return "";
        }
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (IllegalStateException e) {
                System.out.println("\n⚠️  Entrada interrompida. Encerrando...");
                cleanup();
                System.exit(0);
                return -1;
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido.");
            }
        }
    }

    private static void cleanup() {
        System.out.println("\n🔄 Encerrando serviços...");
        
        try {
            // Fecha o scanner primeiro
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception e) {
                    System.err.println("\n❌ Erro ao fechar scanner: " + e.getMessage());
                }
            }

            // Fecha os serviços em ordem
            if (userService != null) {
                try {
                    userService.close();
                } catch (Exception e) {
                    System.err.println("\n❌ Erro ao fechar UserService: " + e.getMessage());
                }
            }

            if (mongoDBService != null) {
                try {
                    mongoDBService.close();
                } catch (Exception e) {
                    System.err.println("\n❌ Erro ao fechar MongoDBService: " + e.getMessage());
                }
            }

            // Força o encerramento de todas as threads não-daemon
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            
            for (Thread thread : threads) {
                if (thread != null && !thread.isDaemon() && thread != Thread.currentThread()) {
                    try {
                        thread.interrupt();
                        // Aguarda um pouco para a thread terminar
                        thread.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Aguarda um pouco para garantir que tudo seja finalizado
            Thread.sleep(1000);
            
            System.out.println("✅ Serviços encerrados com sucesso!");
        } catch (Exception e) {
            System.err.println("\n❌ Erro durante o encerramento: " + e.getMessage());
        }
    }
} 