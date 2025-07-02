package com.api.qualcy.docs.onlyoffice;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.qualcy.docs.security.AppConfig;

@RestController
public class CallbackController {

   
	 private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);
	 
	   @Value("${onlyoffice.storage.path}")
	    private String storagePath;
	   @Autowired
		private DocumentLockService lockService;
	   
    @CrossOrigin(origins = "*")
    @PostMapping("/save")
    public ResponseEntity<?> saveDocument(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @RequestParam(name = "filename", required = false, defaultValue = "sample_v02.docx") String fileName, 
    									@RequestParam(name = "srcFile", required = false, defaultValue = "sample_v01.docx") String srcFile){
    	 
    	 try {
    			//logger.info(status+" Inside save call back file name "+fileName+ " Source file "+srcFile);
            // Verify JWT
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
            	String token = authHeader.substring(7);
            	logger.info( " Token is  "+token);    
            }

            int status = (Integer) body.get("status");
        	logger.info(status+" Inside save call back file name "+fileName+ " Source file "+srcFile);
            if(status > 1) {
            	lockService.unlock(srcFile, "Subash Rout");
            }
        
            if (status == 2 || status == 3 || status == 6 || status == 7) { // 2 = ready for saving, 3 = corrupted
                String downloadUri = (String) body.get("url");
                Path path = Paths.get(System.getProperty("user.home"), storagePath, fileName);
                logger.info("  %%%%%%%%  "+path);
                try (InputStream in = new URL(downloadUri).openStream()) {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                }

                return ResponseEntity.ok(Map.of("error", 0));
            }else {
            	 return ResponseEntity.ok(Map.of("error", 0));
            }

        } catch (Exception e) {
        	 logger.error("  %%%%%%%%  "+e.getMessage());
           // e.printStackTrace();
            return ResponseEntity.ok(Map.of("error", 1));
        }

       
    }
}

