package com.api.qualcy.docs;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.qualcy.docs.onlyoffice.JwtUtil;

@RestController
public class QualcyHealthCheck {
	private static final Logger logger = LogManager.getLogger(QualcyHealthCheck.class);
	
	   @Value("${server.port}")
	    private String storagePath;
	 
	   
	   @Autowired
		private JwtUtil jwtUtil;
	   
	@GetMapping("/token")
	public String welccome(@RequestParam String userName, String userId, String roleId) {
	//	AppConfig.
	    Map<String, Object> permissions = Map.of(
	            "name", userName,
	            "id",userId,
	            "role", roleId
	        );
		  
		String token = jwtUtil.sign(permissions);
		logger.info(token);
	     Map claimsMap=  jwtUtil.verify(token);
	     logger.info(claimsMap+" claimsMap   "+claimsMap.get("userName"));
		return token;
	}

}
