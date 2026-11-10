package org.lambda.example.app;

import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.lambda.framework.nacos.config.RsocketRunListener;
import org.lambda.framework.rpc.adapter.RsocketRpcProxyBeanFactoryPostProcessor;
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
import reactor.core.publisher.Mono;

@SpringBootApplication
@Import({RSocketStrategiesConfig.class, RSocketAcceptorInterceptor.class, RsocketGlobalExceptionHandler.class,
        RsocketInterceptorConfig.class, RsocketRunListener.class, RSocketLoadbalance.class,
        RsocketRpcProxyBeanFactoryPostProcessor.class})
public class Application {
    @Bean
    RSocketMessageHandlerCustomizer requestValidation(Validator validator) {
        return handler -> handler.setValidator(validator);
    }

    @Bean
    PrincipalStash rpcPrincipalStash() {
        return () -> Mono.deferContextual(context -> Mono.justOrEmpty(context.<SecurityStash>getOrEmpty(SecurityStash.class)));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
