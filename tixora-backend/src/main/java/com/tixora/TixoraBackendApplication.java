package com.tixora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TixoraBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TixoraBackendApplication .class, args);
	}
}