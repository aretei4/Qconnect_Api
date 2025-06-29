package com.api.qualcy.docs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.qualcy.docs.onlyoffice.JwtUtil;

@RestController
public class QualcyHealthCheck {
	private static final Logger logger = LogManager.getLogger(QualcyHealthCheck.class);
	
	   @Value("${server.port}")
	    private String storagePath;
	 
	   

	@GetMapping("/HealthCheck")
	public String welccome() {
	//	AppConfig.
		System.out.println( "  ****************   "+storagePath);
		logger.debug("debug started");
		return "welcome qualcy system";
	}

}
