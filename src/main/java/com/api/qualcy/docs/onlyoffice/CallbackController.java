package com.api.qualcy.docs.onlyoffice;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
       
    	System.out.println("  service called %%%%%%%%%%%%%%%%%%%%%%%%%%%%   ");
    	/* try {
            // Verify JWT
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                System.out.println(token);
               // jwtUtil.verify(token); // throws if invalid
            }

            int status = (Integer) body.get("status");
            if (status == 2 || status == 3) { // 2 = ready for saving, 3 = corrupted
                String downloadUri = (String) body.get("url");
                System.out.println(downloadUri);
                Path path = Paths.get(System.getProperty("user.home"), "data", "example.docx");
             //   String fileName = "example.docx";
              //  Path file = Paths.get("./storage").resolve(filename);
                //Path path = Paths.get(storagePath, fileName);
                System.out.println("  %%%%%%%%  "+path);
                try (InputStream in = new URL(downloadUri).openStream()) {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                }

                return ResponseEntity.ok(Map.of("error", 0));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return ResponseEntity.ok(Map.of("error", 1));
    }
}

