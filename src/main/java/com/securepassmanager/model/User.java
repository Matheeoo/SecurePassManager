package com.securepassmanager.model;

public class User {
    private String id;
    private String email;
    private String passwordHash;
    private String totpSecret;

    public User() {}

    public User(String email, String passwordHash, String totpSecret) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.totpSecret = totpSecret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }
} 