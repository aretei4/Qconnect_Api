package com.api.qualcy.docs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSample {

	public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read JSON file into Map
        File file = new File("src/main/resources/onlyoffice.json");
        Map<String, Object> jsonMap = mapper.readValue(file, Map.class);

        // Example: access values
        Map<String, Object> document = (Map<String, Object>) jsonMap.get("document");
        String title = (String) document.get("title");

        System.out.println("Document title: " + title);
    }

}
