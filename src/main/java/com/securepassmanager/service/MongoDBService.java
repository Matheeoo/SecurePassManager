package com.securepassmanager.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.securepassmanager.model.PasswordEntry;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MongoDBService {
    private static final String DATABASE_NAME = "SecurePassManager";
    private static final String COLLECTION_NAME = "passwords";
    private static final String MASTER_COLLECTION = "master_password";
    private static final int CLOSE_TIMEOUT_SECONDS = 5;

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private volatile boolean isClosed = false;
    private final boolean isCloud;

    public MongoDBService() {
        Properties props = new Properties();
        String cloudUri = null;
        String localUri = null;
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            props.load(fis);
            cloudUri = props.getProperty("mongodb.uri.cloud");
            localUri = props.getProperty("mongodb.uri.local");
        } catch (IOException e) {
            System.err.println("Não foi possível ler application.properties. Usando padrão local.");
            localUri = "mongodb://localhost:27017";
        }

        MongoClient client = null;
        MongoDatabase db = null;
        MongoCollection<Document> coll = null;
        boolean cloud = false;

        // Tenta conectar na nuvem primeiro
        if (cloudUri != null && !cloudUri.contains("<usuario>")) {
            try {
                client = MongoClients.create(cloudUri);
                db = client.getDatabase(DATABASE_NAME);
                coll = db.getCollection(COLLECTION_NAME);
                db.runCommand(new Document("ping", 1));
                cloud = true;
                System.out.println("Conectado ao MongoDB Atlas (nuvem).");
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg.contains("Authentication failed")) {
                    System.out.println("Erro: Usuário ou senha do MongoDB Atlas incorretos.");
                } else if (msg.contains("timed out") || msg.contains("Timeout")) {
                    System.out.println("Erro: Timeout de conexão. Verifique sua internet ou se o IP está liberado no Atlas.");
                } else if (msg.contains("not authorized")) {
                    System.out.println("Erro: Usuário não tem permissão para acessar o banco.");
                } else if (msg.contains("UnknownHostException")) {
                    System.out.println("Erro: String de conexão inválida ou DNS do cluster incorreto.");
                } else if (msg.contains("No suitable servers found")) {
                    System.out.println("Erro: Não foi possível encontrar servidores MongoDB. Verifique a string de conexão e o status do cluster.");
                } else {
                    System.out.println("Erro ao conectar no Atlas: " + msg);
                }
                e.printStackTrace();
                System.out.println("Falha ao conectar ao MongoDB Atlas. Tentando local...");
            }
        }
        if (client == null) {
            try {
                client = MongoClients.create(localUri);
                db = client.getDatabase(DATABASE_NAME);
                coll = db.getCollection(COLLECTION_NAME);
                db.runCommand(new Document("ping", 1));
                System.out.println("Conectado ao MongoDB local.");
            } catch (Exception ex) {
                throw new RuntimeException("Não foi possível conectar ao MongoDB local nem à nuvem.", ex);
            }
        }
        this.mongoClient = client;
        this.database = db;
        this.collection = coll;
        this.isCloud = cloud;
    }

    public boolean isCloudConnection() {
        return isCloud;
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

    // Insere ou atualiza uma entrada de senha (upsert)
    public void insertOrUpdatePasswordEntry(PasswordEntry entry) {
        validateConnection();
        Bson filter = Filters.and(
            Filters.eq("service", entry.getService()),
            Filters.eq("userId", entry.getUserId())
        );
        Document doc = new Document()
                .append("title", entry.getTitle())
                .append("service", entry.getService())
                .append("username", entry.getUsername())
                .append("password", entry.getPassword())
                .append("userId", entry.getUserId())
                .append("createdAt", entry.getCreatedAt().toString())
                .append("updatedAt", entry.getUpdatedAt().toString());
        collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    // Substitui todas as senhas de um usuário por uma nova lista
    public void replaceAllPasswordEntries(String userId, List<PasswordEntry> entries) {
        validateConnection();
        collection.deleteMany(Filters.eq("userId", userId));
        for (PasswordEntry entry : entries) {
            insertPasswordEntry(entry);
        }
    }
} 