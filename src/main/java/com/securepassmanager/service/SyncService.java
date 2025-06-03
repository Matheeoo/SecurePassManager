package com.securepassmanager.service;

import com.securepassmanager.model.PasswordEntry;
import java.util.*;

public class SyncService {
    private final MongoDBService localService;
    private final MongoDBService cloudService;

    public SyncService(MongoDBService localService, MongoDBService cloudService) {
        this.localService = localService;
        this.cloudService = cloudService;
    }

    // Sincronização bidirecional com merge por updatedAt
    public void syncBidirectional(String userId) {
        List<PasswordEntry> localEntries = localService.getAllPasswordEntries(userId);
        List<PasswordEntry> cloudEntries = cloudService.getAllPasswordEntries(userId);

        // Mapear por serviço para facilitar o merge
        Map<String, PasswordEntry> localMap = new HashMap<>();
        for (PasswordEntry entry : localEntries) {
            localMap.put(entry.getService(), entry);
        }
        Map<String, PasswordEntry> cloudMap = new HashMap<>();
        for (PasswordEntry entry : cloudEntries) {
            cloudMap.put(entry.getService(), entry);
        }

        // Merge: mantém sempre o mais recente
        Set<String> allServices = new HashSet<>();
        allServices.addAll(localMap.keySet());
        allServices.addAll(cloudMap.keySet());

        List<PasswordEntry> merged = new ArrayList<>();
        for (String service : allServices) {
            PasswordEntry local = localMap.get(service);
            PasswordEntry cloud = cloudMap.get(service);
            if (local == null) {
                merged.add(cloud);
            } else if (cloud == null) {
                merged.add(local);
            } else {
                // Compara updatedAt
                if (local.getUpdatedAt().isAfter(cloud.getUpdatedAt())) {
                    merged.add(local);
                } else {
                    merged.add(cloud);
                }
            }
        }

        // Atualiza ambos os bancos com o merge
        localService.replaceAllPasswordEntries(userId, merged);
        cloudService.replaceAllPasswordEntries(userId, merged);
        System.out.println("Sincronização bidirecional com merge concluída!");
    }
} 