package com.rachel.ledgercore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class LedgercoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgercoreApplication.class, args);
	}

}
