package com.halcyon.recurix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecurixBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecurixBotApplication.class, args);
    }
}
