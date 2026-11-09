package org.lambda.example.app;

import org.lambda.framework.nacos.config.RsocketRunListener;
import org.lambda.framework.rsocket.adapter.RSocketAcceptorInterceptor;
import org.lambda.framework.rsocket.adapter.RSocketStrategiesConfig;
import org.lambda.framework.rsocket.adapter.RsocketGlobalExceptionHandler;
import org.lambda.framework.rsocket.adapter.RsocketInterceptorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.rsocket.autoconfigure.RSocketMessageHandlerCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.validation.Validator;

@SpringBootApplication
@Import({RSocketStrategiesConfig.class, RSocketAcceptorInterceptor.class, RsocketGlobalExceptionHandler.class,
        RsocketInterceptorConfig.class, RsocketRunListener.class})
public class Application {
    @Bean
    RSocketMessageHandlerCustomizer requestValidation(Validator validator) {
        return handler -> handler.setValidator(validator);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
