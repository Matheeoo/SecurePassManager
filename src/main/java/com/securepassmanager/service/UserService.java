package com.securepassmanager.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.securepassmanager.model.User;
import org.bson.Document;
import org.bson.types.ObjectId;

public class UserService {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "SecurePassManager";
    private static final String COLLECTION_NAME = "users";

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public UserService() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    public void registerUser(User user) {
        Document doc = new Document()
                .append("email", user.getEmail())
                .append("passwordHash", user.getPasswordHash())
                .append("totpSecret", user.getTotpSecret());
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
            return user;
        }
        return null;
    }

    public void close() {
        mongoClient.close();
    }
} 