package com.api.distr.docs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QualcyApplication {

	public static void main(String[] args) {
		 System.setProperty("user.timezone", "Asia/Kolkata");
		SpringApplication.run(QualcyApplication.class, args);
	}

}
