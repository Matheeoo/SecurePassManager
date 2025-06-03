# 🔐 SecurePassManager

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Security](https://img.shields.io/badge/Security-AES%2Fbcrypt-yellow.svg)](https://www.bouncycastle.org/)
[![2FA](https://img.shields.io/badge/2FA-TOTP%20%7C%20QR%20Code-blueviolet.svg)](https://github.com/google/google-authenticator)

Um gerenciador de senhas seguro e moderno desenvolvido em Java, oferecendo recursos avançados de segurança e uma interface intuitiva para gerenciar suas credenciais de forma segura.

## ✨ Funcionalidades Principais

- 🔒 **Armazenamento Seguro**
  - Criptografia AES-256-GCM para senhas
  - Hashing bcrypt para senhas mestras
  - Proteção contra ataques de força bruta
  - Gerenciamento seguro de chaves de criptografia

- 🔐 **Autenticação Avançada**
  - Autenticação de dois fatores (2FA) via TOTP
  - Códigos de backup para recuperação
  - QR Code para configuração fácil do 2FA
  - Proteção contra tentativas de login maliciosas

- 🛡️ **Segurança Proativa**
  - Verificação de vazamentos via HaveIBeenPwned API
  - Gerador de senhas fortes e personalizáveis
  - Validação de força de senhas
  - Proteção contra SQL Injection

- 👥 **Multi-usuário**
  - Suporte a múltiplos usuários
  - Isolamento de dados entre usuários
  - Gerenciamento de permissões

## 🚀 Começando

### Pré-requisitos

- Java 17 ou superior
- Maven 3.6 ou superior
- MongoDB 4.4 ou superior (rodando localmente na porta 27017)

### Instalação

1. Clone o repositório:
   ```bash
   git clone https://github.com/Matheeoo/SecurePassManager
   cd SecurePassManager
   ```

2. Compile o projeto:
   ```bash
   mvn clean install
   ```

3. Execute o aplicativo:
   ```bash
   mvn exec:java -Dexec.mainClass="com.securepassmanager.Main"
   ```

## 📁 Estrutura do Projeto

```
src/
├── main/
│   └── java/
│       └── com/
│           └── securepassmanager/
│               ├── model/      # Entidades e DTOs
│               ├── service/    # Lógica de negócios
│               ├── security/   # Criptografia e segurança
│               ├── util/       # Utilitários
│               └── api/        # Integrações externas
└── test/
    └── java/
        └── com/
            └── securepassmanager/
                └── ...         # Testes unitários e de integração
```

## 🔧 Configuração

O sistema utiliza as seguintes configurações padrão:

- MongoDB: `mongodb://localhost:27017`
- Banco de dados: `SecurePassManager`
- Coleções: `users`, `passwords`, `master_password`

Para personalizar estas configurações, crie um arquivo `application.properties` na raiz do projeto.

## 🛡️ Segurança

### Criptografia
- AES-256-GCM para criptografia de senhas
- bcrypt para hashing de senhas mestras
- Geração segura de chaves de criptografia
- Proteção contra ataques de força bruta

### Autenticação
- 2FA via TOTP (Time-based One-Time Password)
- Códigos de backup para recuperação
- Bloqueio temporário após tentativas falhas
- Validação de força de senhas

## 🤝 Contribuindo

1. Faça um Fork do projeto
2. Crie uma Branch para sua Feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'feat: add some amazing feature'`)
4. Push para a Branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 📧 Contato

Matheus José - matheusjose1305@gmail.com

