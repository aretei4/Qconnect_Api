package com.api.qualcy.docs.onlyoffice;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallbackController {

   // @Value("${onlyoffice.storage.folder}")
    private String storagePath="./storage";

    @Autowired
    private JwtUtil jwtUtil;

    @CrossOrigin(origins = "*")
    @PostMapping("/save")
    public ResponseEntity<?> saveDocument(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
       
    	String fileName = "sample.docx";
    	System.out.println("  service called %%%%%%%%%%%%%%%%%%%%%%%%%%%%   ");
    	 
    	 try {
    		
            // Verify JWT
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
            	String token = authHeader.substring(7);
            	System.out.println(token);
                Map claimsMap=  jwtUtil.verify(token);
                Map usermap = (Map)((Map)claimsMap.get("editorConfig")).get("user");
                fileName = (String)usermap.get("fileName");
                List<String> users = (List<String>) body.get("users");
                System.out.println(users+"%%%%%%%%%%%%  file name "+fileName); 
               
            }

            int status = (Integer) body.get("status");
            if (status == 2 || status == 6 || status == 7) { // 2 = ready for saving, 3 = corrupted
                String downloadUri = (String) body.get("url");
                System.out.println(downloadUri);
                Path path = Paths.get(System.getProperty("user.home"), "data", fileName);
                System.out.println("  %%%%%%%%  "+path);
                try (InputStream in = new URL(downloadUri).openStream()) {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                }

                return ResponseEntity.ok(Map.of("error", 0));
            }else {
            	 return ResponseEntity.ok(Map.of("error", 0));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("error", 1));
        }

       
    }
}

