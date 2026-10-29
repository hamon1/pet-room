package com.petroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PetRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetRoomApplication.class, args);
    }
}
