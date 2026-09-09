package com.sunmoon.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // AcceptanceTimeout sweeps for orders nobody took
public class SunMoonApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunMoonApplication.class, args);
    }
}
