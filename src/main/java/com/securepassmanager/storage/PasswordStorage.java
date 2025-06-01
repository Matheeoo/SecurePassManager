package com.securepassmanager.storage;

import com.securepassmanager.model.PasswordEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PasswordStorage {
    private static final String STORAGE_FILE = "passwords.json";
    private static final String MASTER_PASSWORD_FILE = "master_password.json";
    private final Gson gson;
    private List<PasswordEntry> passwords;

    public PasswordStorage() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        this.passwords = new ArrayList<>();
        loadPasswords();
    }

    public void savePasswords() {
        try (FileWriter writer = new FileWriter(STORAGE_FILE)) {
            gson.toJson(passwords, writer);
        } catch (IOException e) {
            System.err.println("Erro ao salvar senhas: " + e.getMessage());
        }
    }

    private void loadPasswords() {
        File file = new File(STORAGE_FILE);
        if (!file.exists() || file.length() == 0) {
            passwords = new ArrayList<>();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<PasswordEntry>>(){}.getType();
            List<PasswordEntry> loadedPasswords = gson.fromJson(reader, type);
            passwords = loadedPasswords != null ? loadedPasswords : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Erro ao carregar senhas: " + e.getMessage());
            passwords = new ArrayList<>();
        }
    }

    public void saveMasterPassword(String hashedMasterPassword) {
        try (FileWriter writer = new FileWriter(MASTER_PASSWORD_FILE)) {
            gson.toJson(hashedMasterPassword, writer);
        } catch (IOException e) {
            System.err.println("Erro ao salvar senha mestra: " + e.getMessage());
        }
    }

    public String loadMasterPassword() {
        File file = new File(MASTER_PASSWORD_FILE);
        if (!file.exists() || file.length() == 0) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, String.class);
        } catch (Exception e) {
            System.err.println("Erro ao carregar senha mestra: " + e.getMessage());
            return null;
        }
    }

    public void addPassword(PasswordEntry entry) {
        passwords.add(entry);
        savePasswords();
    }

    public List<PasswordEntry> getAllPasswords() {
        return new ArrayList<>(passwords);
    }

    public PasswordEntry findPassword(String service) {
        return passwords.stream()
                .filter(p -> p.getService().equalsIgnoreCase(service))
                .findFirst()
                .orElse(null);
    }

    public void removePassword(String service) {
        passwords.removeIf(p -> p.getService().equalsIgnoreCase(service));
        savePasswords();
    }
} 