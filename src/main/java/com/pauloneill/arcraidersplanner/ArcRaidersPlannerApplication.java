package com.pauloneill.arcraidersplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class ArcRaidersPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArcRaidersPlannerApplication.class, args);
    }
}
