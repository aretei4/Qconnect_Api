package com.api.qualcy.docs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSample {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Path p = Paths.get(System.getProperty("user.home"), "data", "file.csv");
		
		System.out.println("  *************** "+p);
		
	}

}
