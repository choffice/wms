package com.portfolio.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WarehouseBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseBackendApplication.class, args);
    }
}
