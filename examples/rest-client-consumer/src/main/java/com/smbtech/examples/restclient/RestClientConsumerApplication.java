package com.smbtech.examples.restclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(RestClientConsumerRuntimeHints.class)
public class RestClientConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestClientConsumerApplication.class, args);
    }
}
