package org.lambda.example.gateway;

import org.lambda.framework.gateway.adapter.GatewayGlobalExceptionHandler;
import org.lambda.framework.gateway.filter.GatewayFilter;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.lambda.framework.gateway.support.GatewayPrincipalStash;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({GatewayFilter.class, RsocketRequestFactory.class, RSocketLoadbalance.class,
        GatewayGlobalExceptionHandler.class, GatewayPrincipalStash.class})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
