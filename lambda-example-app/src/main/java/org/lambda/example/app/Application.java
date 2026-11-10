package org.lambda.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.rsocket.autoconfigure.RSocketMessageHandlerCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
public class Application {
    @Bean
    RSocketMessageHandlerCustomizer requestValidation(Validator validator) {
        return handler -> handler.setValidator(validator);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
