# SecurePassManager

Um gerenciador de senhas seguro desenvolvido em Java com recursos avançados de segurança.

## Funcionalidades

- Cadastro de senhas para diferentes serviços
- Criptografia AES/bcrypt para armazenamento seguro
- Autenticação de dois fatores (2FA)
- Gerador de senhas seguras
- Verificação de vazamentos de senhas via API HaveIBeenPwned

## Requisitos

- Java 17 ou superior
- Maven 3.6 ou superior

## Estrutura do Projeto

```
src/
├── main/
│   └── java/
│       └── com/
│           └── securepassmanager/
│               ├── model/      # Classes de modelo
│               ├── service/    # Lógica de negócios
│               ├── util/       # Utilitários
│               ├── security/   # Criptografia e segurança
│               └── api/        # Integrações com APIs externas
└── test/
    └── java/
        └── com/
            └── securepassmanager/
                └── ...         # Testes unitários e de integração
```

## Instalação

1. Clone o repositório
2. Execute `mvn clean install`
3. Execute `mvn exec:java -Dexec.mainClass="com.securepassmanager.Main"`

## Segurança

- Todas as senhas são armazenadas de forma criptografada
- Implementação de 2FA para acesso ao aplicativo
- Verificação de senhas contra vazamentos conhecidos
- Proteção contra SQL Injection e outras vulnerabilidades comuns

## Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo LICENSE para detalhes. 