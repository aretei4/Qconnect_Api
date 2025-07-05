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
	private String baseCallbackUrl;

	@Value("${onlyoffice.download.url}")
	private String downLoadUrl;

	@Value("${onlyoffice.edit.message}")
	private String blockMessage;

	@Autowired
	private DocumentLockService lockService;

	private String userName = "Qualcy Admin";

	@PostMapping("/onlyoffice/config")
	@ResponseBody
	public Map<String, Object> getOnlyOfficeConfig(@RequestBody Map<String, Object> body) {
		boolean viewFlag = false;// (boolean) body.get("edit");
		String downloadName = (String) body.get("fileUrl");
		String destFile = (String) body.get("destFile");
		logger.info("  Storage path Is  " + storagePath);
		logger.info("  Destination file " + destFile);
		Map<String, Object> user = getUserInfo(body);
		if (null != destFile && (destFile.isEmpty() || destFile.isBlank())) {
			return viewConfig(body);
		} else {
			Map<String, Object> qualcy = lockUser(downloadName);
			if (null != qualcy) {
				Map<String, Object> jsonMap = viewConfig(body);
				jsonMap.put("qualcy", qualcy);
				return jsonMap;
			}
			return editConfig(body);
		}

	}

	private Map<String, Object> getUserInfo(Map<String, Object> body) {
		String inToken = (String) body.get("token");
		Map<String, Object> claimsMap = null;
		if (null == inToken || inToken.isEmpty()) {
			userName="Subash Rout";
			claimsMap = new HashMap<String, Object>();
			claimsMap.put("name",userName);
			claimsMap.put("id", "2345");
			
		} else {
			claimsMap = jwtUtil.verify(inToken);
			userName = (String) claimsMap.get("name");
		}
		return claimsMap;
	}

	private Map<String, Object> editConfig(Map<String, Object> body) {
		Map<String, Object> user = getUserInfo(body);
		logger.info(user + " claimsMap   " + user.get("userName"));

		String callbackUrl = baseCallbackUrl;

		ObjectMapper mapper = new ObjectMapper();
		String fileName = "sample.json"; // "onlyoffice.json";
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		try {

			String downloadName = (String) body.get("fileUrl");
			String fileUrl = downLoadUrl + downloadName;
			String destFile = (String) body.get("destFile");

			if (null != destFile && destFile.isEmpty()) {
				callbackUrl = callbackUrl + "sample_v03.docx";
			} else {
				callbackUrl = callbackUrl + destFile;
			}
			callbackUrl = callbackUrl + "&srcFile=" + downloadName;
			logger.info(fileUrl);
			Map<String, Object> jsonMap = mapper.readValue(inputStream, Map.class);
			Map<String, Object> document = (Map<String, Object>) jsonMap.get("document");
			document.put("key", UUID.randomUUID().toString());
			document.put("url", fileUrl);

			Map<String, Object> editorConfig = (Map<String, Object>) jsonMap.get("editorConfig");
			editorConfig.put("callbackUrl", callbackUrl);
			editorConfig.put("user", user);
			jsonMap.put("editorConfig", editorConfig);

			String token = jwtUtil.sign(jsonMap);
			logger.info(token);
			jsonMap.put("token", token);
			return jsonMap;
		} catch (Exception e) {
			logger.error("  %%%%%%%%  " + e.getMessage());
			e.printStackTrace();
		}

		Map<String, Object> config = new HashMap<>();

		return config;
	}

	private Map<String, Object> lockUser(String docuId) {
		logger.info(" Locked File name:  " + docuId);
		if (lockService.isLockedByAnotherUser(docuId, docuId)) {
			String lockUserName = lockService.getLockedUser(docuId);
			Map<String, Object> qualcy = new HashMap<String, Object>();
			String message = String.format(blockMessage, lockUserName);
			qualcy.put("errorMsg", message);
			qualcy.put("isBlock", true);
			return qualcy;
		} else {
			lockService.tryLock(docuId, userName);
			return null;
		}

	}

	private Map<String, Object> viewConfig(Map<String, Object> body) {
		Map<String, Object> user = getUserInfo(body);
		ObjectMapper mapper = new ObjectMapper();

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("onlyoffice_view.json");

		try {
			String fileUrl = downLoadUrl + (String) body.get("fileUrl");

			logger.info(fileUrl);
			Map<String, Object> jsonMap = mapper.readValue(inputStream, Map.class);
			Map<String, Object> document = (Map<String, Object>) jsonMap.get("document");
			document.put("key", UUID.randomUUID().toString());
			document.put("url", fileUrl);
			Map<String, Object> editorConfig = (Map<String, Object>) jsonMap.get("editorConfig");
			editorConfig.put("user", user);
			jsonMap.put("editorConfig", editorConfig);
			
			String token = jwtUtil.sign(jsonMap);
			logger.info(token);
			jsonMap.put("token", token);

			return jsonMap;
		} catch (Exception e) {
			logger.error("  %%%%%%%%  " + e.getMessage());
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
			logger.info("  Storage path Is  " + storagePath);

			Path file = Paths.get(System.getProperty("user.home"), storagePath, filename);
			Resource resource = new UrlResource(file.toUri());
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(resource);
		} catch (Exception e) {
			logger.error("  %%%%%%%%  " + e.getMessage());
			return ResponseEntity.status(503).body(null);
			// e.printStackTrace();
		}
	}

}
