package com.api.qualcy.docs.onlyoffice;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
public class OnlyOfficeEdit {
	
	 private static final Logger logger = LoggerFactory.getLogger(OnlyOfficeEdit.class);
	
	   @Autowired
	    private JwtUtil jwtUtil;
	   
	   @Value("${onlyoffice.storage.path}")
	    private String storagePath;
	   
	   @Value("${onlyoffice.callback.url}")
	    private  String baseCallbackUrl;
	    
			   
	   @PostMapping("/onlyoffice/config")
	   @ResponseBody
	   public Map<String, Object> getOnlyOfficeConfig(@RequestBody Map<String, Object> body) {
		   boolean viewFlag  = false;//(boolean) body.get("edit");
		   String destFile = (String) body.get("destFile");
		   logger.info("  Storage path Is  "+storagePath);
		   logger.info("  Destination file "+destFile);
		   if(null !=destFile && (destFile.isEmpty() || destFile.isBlank())) {
			   return viewConfig(body);
		   }else {
			   return editConfig(body);
		   }
		  
		  
		}
	   
	  private Map<String, Object> editConfig(Map<String, Object> body) {
		  
		  String callbackUrl = baseCallbackUrl;
		   
		   ObjectMapper mapper = new ObjectMapper();
		   String fileName ="sample.json"; //"onlyoffice.json";
		   InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		 
	        try {
	        	String fileUrl = (String) body.get("fileUrl");
	        	String destFile = (String) body.get("destFile");
	        	if(null !=destFile && destFile.isEmpty()) {
	        		callbackUrl = callbackUrl+"sample_v03.docx";
	        	}else {
	        		callbackUrl = callbackUrl+destFile;
	        	}
               logger.info(fileUrl);
				Map<String, Object> jsonMap = mapper.readValue(inputStream, Map.class);
				 Map<String, Object> document = (Map<String, Object>)jsonMap.get("document");
				 document.put("key", UUID.randomUUID().toString());
				 document.put("url", fileUrl); 
				// jsonMap.put("document", document);
				 
				 Map<String, Object> editorConfig = (Map<String, Object>)jsonMap.get("editorConfig");
				 editorConfig.put("callbackUrl", callbackUrl);
				 jsonMap.put("editorConfig", editorConfig);
				 String token = jwtUtil.sign(jsonMap);
				  logger.info(token);
				  jsonMap.put("token", token);
				  Map<String, Object> qualcy = new HashMap<String, Object>(); 
				  qualcy.put("errorMsg", "Item is blocked ");
				  qualcy.put("isBlock", false);
				  jsonMap.put("qualcy", qualcy);
				//jsonMap.put("lockedBy", "Subash Rout");
				 
				return jsonMap;
			}  catch (Exception e) {
				 logger.error("  %%%%%%%%  "+e.getMessage());
				e.printStackTrace();
			}
	        
	       Map<String, Object> config = new HashMap<>();
	   
	       return config;
	  }

 private Map<String, Object> viewConfig(Map<String, Object> body) {
		  
		   
		   ObjectMapper mapper = new ObjectMapper();
		   
		   InputStream inputStream = getClass().getClassLoader().getResourceAsStream("onlyoffice_view.json");

		 
	        try {
	        	String fileUrl = (String) body.get("fileUrl");
	        	
               logger.info(fileUrl);
				Map<String, Object> jsonMap = mapper.readValue(inputStream, Map.class);
				 Map<String, Object> document = (Map<String, Object>)jsonMap.get("document");
				 document.put("key", UUID.randomUUID().toString());
				 document.put("url", fileUrl); 
				 String token = jwtUtil.sign(jsonMap);
				 logger.info(token);
				jsonMap.put("token", token);
				//jsonMap.put("lockedBy", "Subash Rout");
				 
				return jsonMap;
			}  catch (Exception e) {
				 logger.error("  %%%%%%%%  "+e.getMessage());
				e.printStackTrace();
			}
	        
	       Map<String, Object> config = new HashMap<>();
	   
	       return config;
	  }

 
	// For file serving
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    	// Using Paths (modern)
    	try {
    		 logger.info("  Storage path Is  "+storagePath);
    		 
    	Path file = Paths.get(System.getProperty("user.home"), storagePath, filename);
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }  catch (Exception e) {
		 logger.error("  %%%%%%%%  "+e.getMessage());
		 return ResponseEntity.status(503).body(null);
		//e.printStackTrace();
	}
    }

}
