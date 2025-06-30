package com.api.qualcy.docs.onlyoffice;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class DocumentLockService {
    private final ConcurrentHashMap<String, String> documentLocks = new ConcurrentHashMap<>();

    public boolean tryLock(String documentId, String userId) {
        return documentLocks.putIfAbsent(documentId, userId) == null;
    }

    public void unlock(String documentId, String userId) {
        documentLocks.computeIfPresent(documentId, (key, currentUserId) -> {
          //  if (currentUserId.equals(userId)) {
            //    return null;
            //}
            return null;
        });
    }

    public boolean isLockedByAnotherUser(String documentId, String userId) {
        String currentUser = documentLocks.get(documentId);
        System.out.println(documentId+"  locket "+currentUser +"    "+userId);
        return currentUser != null;
    }

    public String getLockedUser(String documentId) {
        return documentLocks.get(documentId);
    }
}

