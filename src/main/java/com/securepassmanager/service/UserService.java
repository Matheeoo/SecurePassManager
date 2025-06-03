package com.securepassmanager.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.securepassmanager.model.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UserService {
    private static final String DATABASE_NAME = "SecurePassManager";
    private static final String COLLECTION_NAME = "users";

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final boolean isCloud;

    public UserService() {
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
                System.out.println("Conectado ao MongoDB Atlas (nuvem) [UserService].");
            } catch (Exception e) {
                System.out.println("Falha ao conectar ao MongoDB Atlas. Tentando local...");
            }
        }
        if (client == null) {
            try {
                client = MongoClients.create(localUri);
                db = client.getDatabase(DATABASE_NAME);
                coll = db.getCollection(COLLECTION_NAME);
                db.runCommand(new Document("ping", 1));
                System.out.println("Conectado ao MongoDB local [UserService].");
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

    public void registerUser(User user) {
        Document doc = new Document()
                .append("email", user.getEmail())
                .append("passwordHash", user.getPasswordHash())
                .append("totpSecret", user.getTotpSecret())
                .append("backupCodes", user.getBackupCodes());
        collection.insertOne(doc);
        user.setId(doc.getObjectId("_id").toHexString());
    }

    public User findByEmail(String email) {
        Document doc = collection.find(Filters.eq("email", email)).first();
        if (doc != null) {
            User user = new User();
            user.setId(doc.getObjectId("_id").toHexString());
            user.setEmail(doc.getString("email"));
            user.setPasswordHash(doc.getString("passwordHash"));
            user.setTotpSecret(doc.getString("totpSecret"));
            List<String> codes = new ArrayList<>();
            if (doc.get("backupCodes") instanceof List<?>) {
                for (Object o : (List<?>) doc.get("backupCodes")) {
                    if (o != null) codes.add(o.toString());
                }
            }
            user.setBackupCodes(codes);
            return user;
        }
        return null;
    }

    public void updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("Usuário sem ID não pode ser atualizado");
        }
        collection.updateOne(
            Filters.eq("_id", new ObjectId(user.getId())),
            new Document("$set", new Document()
                .append("passwordHash", user.getPasswordHash())
                .append("totpSecret", user.getTotpSecret())
                .append("backupCodes", user.getBackupCodes())
            )
        );
    }

    public void close() {
        mongoClient.close();
    }
} 