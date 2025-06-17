package com.api.qualcy.docs.onlyoffice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class OnlyOfficeEdit {
	
	   @Autowired
	    private JwtUtil jwtUtil;
	   
	   @Value("${onlyoffice.docserver.url}")
	    private String docServerUrl;
	   
	   
	 //  String fileName = "example.docx";
	  // String fileUrl = "http://13.204.49.246:3000/download";//"http://localhost:3000/files/" + fileName;
	   String callbackUrl = "http://13.204.49.246:3050/save";
			   
	   @PostMapping("/onlyoffice/config")
	   @ResponseBody
	   public Map<String, Object> getOnlyOfficeConfig(@RequestBody Map<String, Object> body) {
		   ObjectMapper mapper = new ObjectMapper();
		   File file = new File("src/main/resources/onlyoffice.json");
	        try {
	        	String fileUrl = (String) body.get("fileUrl");
                System.out.println(fileUrl);
				Map<String, Object> jsonMap = mapper.readValue(file, Map.class);
				 Map<String, Object> document = (Map<String, Object>)jsonMap.get("document");
				 document.put("key", UUID.randomUUID().toString());
				 document.put("url", fileUrl); 
				// jsonMap.put("document", document);
				 
				 Map<String, Object> editorConfig = (Map<String, Object>)jsonMap.get("editorConfig");
				 editorConfig.put("callbackUrl", callbackUrl);
				 jsonMap.put("editorConfig", editorConfig);
				 String token = jwtUtil.sign(jsonMap);
				  System.out.println(token);
				jsonMap.put("token", token);
				  return jsonMap;
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	       Map<String, Object> config = new HashMap<>();
	   
	       return config;
	   }
	   
	   
	@GetMapping("/edit")
	public String editDoc(Model model) throws JsonProcessingException {
	   
	    Map<String, Object> config = new LinkedHashMap<>();
	    config.put("document", Map.of(
	            "fileType", "docx",
	            "key", UUID.randomUUID().toString(),
	           // "title", fileName,
	   //         "url", fileUrl,
	            "permissions",Map.of(
	            "download", true,
	   			"edit", true,
	   			"editCommentAuthorOnly", true,
	   			"fillForms", true,
	   			"modifyContentControl", true,
	   			"modifyFilter", true,
	   			"print", true,
	   			"protect", true,
	   			"deleteCommentAuthorOnly", true
	            )
	    ));
	    config.put("documentType", "word");
	    config.put("editorConfig", Map.of(
	            "callbackUrl", "http://13.204.49.246:3000/save",
	            //"mode", "edit",
	            //"lang", "en",
	            "user", Map.of("id", "user-1", "name", "John Doe"),
	            "permissions",Map.of(
	    	            "download", true,
	    	   			"edit", true,
	    	   			"print",true
	    	            )
	    ));

	    String token = jwtUtil.sign(config);
	    config.put("token", token);
	    System.out.println(token);
	    model.addAttribute("config", new ObjectMapper().writeValueAsString(config));
	    model.addAttribute("docServerUrl", docServerUrl);
	    return "editor";
	}
	
	
	// For file serving
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {
    	// Using Paths (modern)
    	Path file = Paths.get(System.getProperty("user.home"), "data", filename);
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}
