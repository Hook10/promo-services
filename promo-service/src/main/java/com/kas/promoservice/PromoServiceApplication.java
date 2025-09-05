package com.kas.promoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan("com.kas.promoservice")
@SpringBootApplication
public class PromoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromoServiceApplication.class, args);
    }

}
