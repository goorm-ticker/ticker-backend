package com.goorm.ticker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TickerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TickerApplication.class, args);
    }

}
