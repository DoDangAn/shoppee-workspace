package com.andd.DoDangAn.DoDangAn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class DoDangAnApplication {

	public static void main(String[] args) {
		// Launch Spring Boot application
		SpringApplication.run(DoDangAnApplication.class, args);
	}
}