package com.portfolens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortfoLensApplication {
    public static void main(String[] args) { SpringApplication.run(PortfoLensApplication.class, args); }
}
