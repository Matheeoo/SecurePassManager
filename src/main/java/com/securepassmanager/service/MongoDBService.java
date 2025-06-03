package com.securepassmanager.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.securepassmanager.model.PasswordEntry;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MongoDBService {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "SecurePassManager";
    private static final String COLLECTION_NAME = "passwords";
    private static final String MASTER_COLLECTION = "master_password";
    private static final int CLOSE_TIMEOUT_SECONDS = 5;

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private volatile boolean isClosed = false;

    public MongoDBService() {
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            collection = database.getCollection(COLLECTION_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar ao MongoDB: " + e.getMessage(), e);
        }
    }

    private void validateConnection() {
        if (isClosed) {
            throw new IllegalStateException("Conexão com MongoDB está fechada");
        }
        if (mongoClient == null || database == null || collection == null) {
            throw new IllegalStateException("Conexão com MongoDB não está inicializada corretamente");
        }
    }

    public void insertPasswordEntry(PasswordEntry entry) {
        validateConnection();
        try {
            Document doc = new Document()
                    .append("title", entry.getTitle())
                    .append("service", entry.getService())
                    .append("username", entry.getUsername())
                    .append("password", entry.getPassword())
                    .append("userId", entry.getUserId())
                    .append("createdAt", entry.getCreatedAt().toString())
                    .append("updatedAt", entry.getUpdatedAt().toString());
            collection.insertOne(doc);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir senha: " + e.getMessage(), e);
        }
    }

    public List<PasswordEntry> getAllPasswordEntries(String userId) {
        validateConnection();
        List<PasswordEntry> entries = new ArrayList<>();
        try {
            FindIterable<Document> docs = collection.find(Filters.eq("userId", userId));
            for (Document doc : docs) {
                PasswordEntry entry = new PasswordEntry();
                entry.setTitle(doc.getString("title"));
                entry.setService(doc.getString("service"));
                entry.setUsername(doc.getString("username"));
                entry.setPassword(doc.getString("password"));
                entry.setUserId(doc.getString("userId"));
                entries.add(entry);
            }
            return entries;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao recuperar senhas: " + e.getMessage(), e);
        }
    }

    public PasswordEntry findByService(String service, String userId) {
        validateConnection();
        try {
            Bson filter = Filters.and(Filters.eq("service", service), Filters.eq("userId", userId));
            Document doc = collection.find(filter).first();
            if (doc != null) {
                PasswordEntry entry = new PasswordEntry();
                entry.setTitle(doc.getString("title"));
                entry.setService(doc.getString("service"));
                entry.setUsername(doc.getString("username"));
                entry.setPassword(doc.getString("password"));
                entry.setUserId(doc.getString("userId"));
                return entry;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar senha: " + e.getMessage(), e);
        }
    }

    public void saveMasterPassword(String hashedPassword) {
        validateConnection();
        try {
            MongoCollection<Document> masterCollection = database.getCollection(MASTER_COLLECTION);
            masterCollection.deleteMany(new Document());
            Document doc = new Document("hash", hashedPassword);
            masterCollection.insertOne(doc);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar senha mestra: " + e.getMessage(), e);
        }
    }

    public String loadMasterPassword() {
        validateConnection();
        try {
            MongoCollection<Document> masterCollection = database.getCollection(MASTER_COLLECTION);
            Document doc = masterCollection.find().first();
            return doc != null ? doc.getString("hash") : null;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar senha mestra: " + e.getMessage(), e);
        }
    }

    public synchronized void close() {
        if (isClosed) {
            return;
        }

        try {
            if (mongoClient != null) {
                // Marca como fechado antes de iniciar o processo de fechamento
                isClosed = true;

                // Tenta fechar o cliente MongoDB com timeout
                Thread closeThread = new Thread(() -> {
                    try {
                        mongoClient.close();
                    } catch (Exception e) {
                        System.err.println("Erro ao fechar cliente MongoDB: " + e.getMessage());
                    }
                });
                closeThread.start();
                closeThread.join(TimeUnit.SECONDS.toMillis(CLOSE_TIMEOUT_SECONDS));

                // Se a thread ainda estiver viva, força o encerramento
                if (closeThread.isAlive()) {
                    closeThread.interrupt();
                }

                // Aguarda um pouco para garantir que todas as conexões sejam fechadas
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erro ao aguardar fechamento do MongoDB: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao fechar conexão com MongoDB: " + e.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
} 