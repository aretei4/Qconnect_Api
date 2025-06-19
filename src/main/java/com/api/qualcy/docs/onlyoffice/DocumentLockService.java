package com.api.qualcy.docs.onlyoffice;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DocumentLockService {
    private final ConcurrentHashMap<String, String> documentLocks = new ConcurrentHashMap<>();

    public boolean tryLock(String documentId, String userId) {
        return documentLocks.putIfAbsent(documentId, userId) == null;
    }

    public void unlock(String documentId, String userId) {
        documentLocks.computeIfPresent(documentId, (key, currentUserId) -> {
            if (currentUserId.equals(userId)) {
                return null;
            }
            return currentUserId;
        });
    }

    public boolean isLockedByAnotherUser(String documentId, String userId) {
        String currentUser = documentLocks.get(documentId);
        return currentUser != null && !currentUser.equals(userId);
    }

    public String getLockedUser(String documentId) {
        return documentLocks.get(documentId);
    }
}

