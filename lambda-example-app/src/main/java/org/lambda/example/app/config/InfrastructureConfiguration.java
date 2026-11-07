package org.lambda.example.app.config;

import org.lambda.framework.repository.config.mongodb.DefaultReactiveMongoRepositoryConfig;
import org.lambda.framework.repository.config.redis.DefaultReactiveRedisRepositoryConfig;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.lambda.framework.mq.config.rabbitmq.DefaultReactiveRabbitMQConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration(proxyBeanMethods = false)
public class InfrastructureConfiguration {
    @Configuration(proxyBeanMethods = false)
    public static class Mongo extends DefaultReactiveMongoRepositoryConfig {}

    @Configuration(proxyBeanMethods = false)
    public static class Redis extends DefaultReactiveRedisRepositoryConfig {
        @Bean(destroyMethod = "destroy")
        LettuceConnectionFactory managedRedisConnectionFactory(ReactiveRedisOperation operation) {
            return (LettuceConnectionFactory) operation.getConnectionFactory();
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class Rabbit extends DefaultReactiveRabbitMQConfig {}

    @Bean
    TransactionalOperator mongoTransactions(ReactiveMongoTransactionManager manager) {
        return TransactionalOperator.create(manager);
    }
}
