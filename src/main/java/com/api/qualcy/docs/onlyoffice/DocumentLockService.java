package com.api.qualcy.docs.onlyoffice;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentLockService {
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentLockService.class);
	
    private final ConcurrentHashMap<String, String> documentLocks = new ConcurrentHashMap<>();

    public boolean tryLock(String documentId, String userId) {
        return documentLocks.putIfAbsent(documentId, userId) == null;
    }

    public void unlock(String documentId, String userId) {
    	//logger.info(" Trying to  document id "+documentId + "  By User  "+userId);
    	//documentLocks.
    	if(documentLocks.containsKey(documentId)) {
    		logger.info(" Unlock document id "+documentId + "  By User  "+userId);
    		documentLocks.remove(documentId);
    	}
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

