package com.securepassmanager.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.securepassmanager.model.PasswordEntry;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MongoDBService {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "SecurePassManager";
    private static final String COLLECTION_NAME = "passwords";
    private static final String MASTER_COLLECTION = "master_password";

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public MongoDBService() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    public void insertPasswordEntry(PasswordEntry entry) {
        Document doc = new Document()
                .append("title", entry.getTitle())
                .append("service", entry.getService())
                .append("username", entry.getUsername())
                .append("password", entry.getPassword())
                .append("userId", entry.getUserId())
                .append("createdAt", entry.getCreatedAt().toString())
                .append("updatedAt", entry.getUpdatedAt().toString());
        collection.insertOne(doc);
    }

    public List<PasswordEntry> getAllPasswordEntries(String userId) {
        List<PasswordEntry> entries = new ArrayList<>();
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
    }

    public PasswordEntry findByService(String service, String userId) {
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
    }

    public void saveMasterPassword(String hashedPassword) {
        MongoCollection<Document> masterCollection = database.getCollection(MASTER_COLLECTION);
        masterCollection.deleteMany(new Document()); // Remove qualquer senha antiga
        Document doc = new Document("hash", hashedPassword);
        masterCollection.insertOne(doc);
    }

    public String loadMasterPassword() {
        MongoCollection<Document> masterCollection = database.getCollection(MASTER_COLLECTION);
        Document doc = masterCollection.find().first();
        if (doc != null) {
            return doc.getString("hash");
        }
        return null;
    }

    public void close() {
        mongoClient.close();
    }
} 