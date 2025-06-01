package com.securepassmanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService();
    }

    @Test
    void testEncryptAndDecrypt() throws Exception {
        String originalPassword = "TestPassword123!";
        String encrypted = encryptionService.encryptPassword(originalPassword);
        String decrypted = encryptionService.decryptPassword(encrypted);

        assertNotEquals(originalPassword, encrypted);
        assertEquals(originalPassword, decrypted);
    }

    @Test
    void testPasswordHashing() {
        String password = "TestPassword123!";
        String hashed = encryptionService.hashPassword(password);

        assertTrue(encryptionService.verifyPassword(password, hashed));
        assertFalse(encryptionService.verifyPassword("WrongPassword", hashed));
    }

    @Test
    void testStrongPasswordGeneration() {
        String password = encryptionService.generateStrongPassword(12);
        
        assertTrue(password.length() >= 12);
        assertTrue(password.matches(".*[A-Z].*")); // Contém maiúscula
        assertTrue(password.matches(".*[a-z].*")); // Contém minúscula
        assertTrue(password.matches(".*[0-9].*")); // Contém número
        assertTrue(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")); // Contém caractere especial
    }

    @Test
    void testMinimumPasswordLength() {
        String password = encryptionService.generateStrongPassword(4);
        assertTrue(password.length() >= 8); // Deve respeitar o tamanho mínimo
    }
} 