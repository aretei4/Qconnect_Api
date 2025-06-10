package com.api.qualcy.docs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QualcyHealthCheck {
	private static final Logger logger = LogManager.getLogger(QualcyHealthCheck.class);

	@GetMapping("/HealthCheck")
	public String welccome() {
		logger.debug("debug started");
		return "welcome qualcy system";
	}

}
