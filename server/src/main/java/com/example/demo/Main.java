package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		System.out.println("Starting the Document Search API...");
		SpringApplication.run(Main.class, args);
	}

}
