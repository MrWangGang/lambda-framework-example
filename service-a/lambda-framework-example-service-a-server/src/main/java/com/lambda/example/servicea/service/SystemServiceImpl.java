package com.lambda.example.servicea.service;

import com.lambda.example.servicea.contract.SystemContract;
import jakarta.annotation.Resource;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SystemServiceImpl {
    @Resource
    private Environment environment;
    @Resource
    private MongoConnectionDetails mongo;

    public Mono<SystemContract.Info> info(SystemContract.EmptyRequest input) {
        return Mono.fromSupplier(() -> new SystemContract.Info(
                environment.getRequiredProperty("spring.application.name"),
                environment.getRequiredProperty("example.greeting"),
                mongo.getConnectionString().getRequiredReplicaSetName()));
    }
}
