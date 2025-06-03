package com.securepassmanager.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.Base64;

public class KeyManager {
    private static final String KEY_FILE = "secure_key.dat";
    private static final int KEY_SIZE = 256;
    private static final String ALGORITHM = "AES";

    public static SecretKey getOrCreateKey() throws Exception {
        Path keyPath = Paths.get(KEY_FILE);
        
        if (Files.exists(keyPath)) {
            return loadKey(keyPath);
        } else {
            SecretKey key = generateKey();
            saveKey(key, keyPath);
            return key;
        }
    }

    private static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE, SecureRandom.getInstanceStrong());
        return keyGen.generateKey();
    }

    private static void saveKey(SecretKey key, Path path) throws IOException {
        byte[] encoded = key.getEncoded();
        Files.write(path, encoded);
        // Define permissões restritas no arquivo
        Files.setPosixFilePermissions(path, 
            PosixFilePermissions.fromString("rw-------"));
    }

    private static SecretKey loadKey(Path path) throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        return new SecretKeySpec(encoded, ALGORITHM);
    }
} 