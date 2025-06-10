package com.api.qualcy.docs.onlyoffice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;



import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Objects;

@RestController
public class OnlyOfficeCallbackController {
    
    // Replace with your secret key from OnlyOffice config
    private static final String ONLYOFFICE_SECRET = "your-secret-key-here";
   
    @PostMapping("/onlyoffice-callback")
    public ResponseEntity<String> handleCallback(@RequestBody OnlyOfficeCallback callback) {
        // 1. Validate token (if enabled)
        if (ONLYOFFICE_SECRET != null && !ONLYOFFICE_SECRET.isEmpty()) {
            String expectedToken = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, ONLYOFFICE_SECRET)
                    .hmacHex(callback.getKey() + callback.getStatus());
            
            if (!Objects.equals(callback.getToken(), expectedToken)) {
                return ResponseEntity.status(403).body("Invalid token");
            }
        }

        // 2. Process based on status
        switch (callback.getStatus()) {
            case 1: // Document ready for saving
                saveDocument(callback.getUrl(), callback.getKey());
                return ResponseEntity.ok("{\"error\":0}");
                
            case 2: // Document save error
             //   log.error("Document save failed for key: " + callback.getKey());
                return ResponseEntity.ok("{\"error\":0}"); // Acknowledge callback
                
            case 3: // Document closed with no changes
                return ResponseEntity.ok("{\"error\":0}");
                
            case 4: // Document edited and saved
                saveDocument(callback.getUrl(), callback.getKey());
                return ResponseEntity.ok("{\"error\":0}");
                
            default: // Status 0 (editing) or others
                return ResponseEntity.ok("{\"error\":0}");
        }
    }

    private void saveDocument(String documentUrl, String documentKey) {
        try {
            // Download and save the document using your logic
            byte[] fileBytes = downloadFile(documentUrl);
            // Save to storage (e.g., database/filesystem)
            // yourStorageService.save(documentKey, fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save document", e);
        }
    }

    private byte[] downloadFile(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
           /* return httpClient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toByteArray(entity);
            });*/
            
            return "".getBytes();
        }
    }
}
