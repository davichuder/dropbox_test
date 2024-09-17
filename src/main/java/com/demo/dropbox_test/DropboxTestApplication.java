package com.demo.dropbox_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DropboxTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(DropboxTestApplication.class, args);
	}

}
