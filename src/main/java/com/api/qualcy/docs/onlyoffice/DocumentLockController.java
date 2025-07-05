package com.api.qualcy.docs.onlyoffice;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentLockController {
	
	 private static final Logger logger = LoggerFactory.getLogger(DocumentLockController.class);
	 
	   @Value("${onlyoffice.storage.path}")
	    private String storagePath;
	   
	   @Autowired
	    private DocumentLockService documentLockService;
	   
	   @GetMapping("/open-document")
	   public ResponseEntity<?> openDocument(@RequestParam String docId, String userId) {
	   //    String userId = principal.getName();
	       if (documentLockService.isLockedByAnotherUser(docId, userId)) {
	           return ResponseEntity.status(HttpStatus.CONFLICT)
	                   .body("Document is being edited by someone else: " + documentLockService.getLockedUser(docId));
	       }

	       documentLockService.tryLock(docId, userId);
	       return ResponseEntity.ok("Document opened successfully");
	   }

	   @PostMapping("/close-document")
	   public ResponseEntity<?> closeDocument(@RequestParam String docId,String userId) {
	       documentLockService.unlock(docId, userId);
	       return ResponseEntity.ok("Unlocked successfully");
	   }
	   
	   @GetMapping("/doc-status")
	   public ResponseEntity<?> isDocOpened(@RequestParam String docId) {
		   Map<String, Object> lockUser =new HashMap<String,Object>() ;
		   lockUser.put("isEdit",false );			
	       String usrName = documentLockService.getLockedUser(docId);
	       if(null !=usrName && !(usrName.isEmpty())) {
	    	   lockUser.put("isEdit",true );   
	    	   lockUser.put("name",usrName );  
	       }
	       return ResponseEntity.ok(lockUser);
	   }
	   

	   
}
