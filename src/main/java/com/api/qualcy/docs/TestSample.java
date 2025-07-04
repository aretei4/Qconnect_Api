package com.api.qualcy.docs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.api.qualcy.docs.onlyoffice.JwtUtil;
import com.api.qualcy.docs.security.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSample {

	public static void main(String[] args) throws Exception {
		  
		  
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "{"
        	    + "  \"userName\": \"Subash Rout\","
        	    + "  \"roole\": \"view\","
        	    + "  \"userId\": \"7836\","
        	 + "}";
        		  		
        String token ="eyJhbGciOiJIUzI1NiJ9.eyJkb2N1bWVudCI6eyJmaWxlVHlwZSI6ImRvY3giLCJrZXkiOiIzOWY1YWJiZC01YWExLTRmMGQtOTA0Mi1mYWE1MjhlZDk2ZjgiLCJ0aXRsZSI6Ik15IERvY3VtZW50LmRvY3giLCJ1cmwiOiJodHRwOi8vMTMuMjA0LjQ5LjI0NjozMDUwL2ZpbGVzL25ld190ZW1wbGF0ZS5kb2N4IiwicGVybWlzc2lvbnMiOnsiZGVsZXRlQ29tbWVudEF1dGhvck9ubHkiOmZhbHNlLCJkb3dubG9hZCI6dHJ1ZSwiZWRpdCI6dHJ1ZSwiZWRpdENvbW1lbnRBdXRob3JPbmx5IjpmYWxzZSwiZmlsbEZvcm1zIjp0cnVlLCJtb2RpZnlDb250ZW50Q29udHJvbCI6dHJ1ZSwibW9kaWZ5RmlsdGVyIjp0cnVlLCJwcmludCI6dHJ1ZSwicHJvdGVjdCI6dHJ1ZSwicmV2aWV3IjpmYWxzZX19LCJlZGl0b3JDb25maWciOnsiY2FsbGJhY2tVcmwiOiJodHRwOi8vMTMuMjA0LjQ5LjI0NjozMDUwL3NhdmUiLCJ1c2VyIjp7ImlkIjoiU3ViYXNoIiwibmFtZSI6IlN1YmFzaCBSb3V0IiwiZmlsZU5hbWUiOiJzYW1wbGVfdjAxLmRvY3gifSwicGVybWlzc2lvbnMiOnsiZWRpdCI6dHJ1ZSwiZG93bmxvYWQiOnRydWUsInByaW50Ijp0cnVlfSwiY3VzdG9taXphdGlvbiI6eyJhYm91dCI6dHJ1ZSwiYW5vbnltb3VzIjp7InJlcXVlc3QiOnRydWUsImxhYmVsIjoiR3Vlc3QifSwiYXV0b3NhdmUiOnRydWUsImNsb3NlIjp7InZpc2libGUiOnRydWUsInRleHQiOiJDbG9zZSBmaWxlIn19fX0._AovzepyNvgfFvtXjVEALPitZ2khc5Juld5TNhmlawY";
        //Map claimsMap=  JwtUtil.verify(token);
      //  Map claimsMap = (Map)((Map)jwt.getBody().get("editorConfig")).get("user");
        // Read JSON file into Map
      //  File file = new File("src/main/resources/onlyoffice.json");
        //Map<String, Object> jsonMap = mapper.readValue(file, Map.class);

        // Example: access values
        //Map<String, Object> document = (Map<String, Object>) jsonMap.get("document");
        //String title = (String) document.get("title");

       // System.out.println("Document title: " + title);
    }

}
