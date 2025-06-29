package com.awsdemo.awstask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwstaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwstaskApplication.class, args);
	}

}
