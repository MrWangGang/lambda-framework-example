package com.lambda.example.serviceb;

import org.lambda.framework.nacos.config.RsocketRunListener;
import org.lambda.framework.rsocket.adapter.RSocketAcceptorInterceptor;
import org.lambda.framework.rsocket.adapter.RSocketStrategiesConfig;
import org.lambda.framework.rsocket.adapter.RsocketGlobalExceptionHandler;
import org.lambda.framework.rsocket.adapter.RsocketInterceptorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({RSocketStrategiesConfig.class, RSocketAcceptorInterceptor.class, RsocketGlobalExceptionHandler.class,
        RsocketInterceptorConfig.class, RsocketRunListener.class})
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }
}
