package org.lambda.example;

import com.mongodb.reactivestreams.client.MongoClient;
import com.rabbitmq.client.*;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.lambda.framework.repository.config.redis.AbstractReactiveRedisRepositoryConfig;
import org.lambda.framework.repository.config.mongodb.AbstractReactiveMongoRepositoryConfig;
import org.lambda.framework.repository.config.mongodb.DefaultReactiveMongoRepositoryConfig;
import org.lambda.framework.mq.config.rabbitmq.AbstractReactiveRabbitMQConfig;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in tests against real infrastructure. Every resource is uniquely named and removed in finally. */
class InfrastructureIT {
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set " + name + " before running -Pintegration");
        return value;
    }
    private static String host() { return env("SERVICE_HOST"); }
    private static String user() { return env("SERVICE_USER"); }
    private static String password() { return env("SERVICE_PASSWORD"); }
    private static String unique() { return "lambda_it_" + UUID.randomUUID().toString().replace("-", ""); }

    @Test void redisAclRoundTripAndCleanup() {
        var config = new AbstractReactiveRedisRepositoryConfig() {
            protected String host() { return InfrastructureIT.host() + ":6379"; }
            protected String username() { return user(); }
            protected String password() { return InfrastructureIT.password(); }
            protected Integer database() { return 0; }
            protected Integer maxActive() { return 4; }
            protected Integer maxWaitSeconds() { return 5; }
            protected Integer maxIdle() { return 2; }
            protected Integer minIdle() { return 0; }
            protected String deployModel() { return "single"; }
            protected String masterName() { return "master"; }
        };
        var redis = config.buildRedisOperation();
        String key = unique();
        try {
            assertEquals(Boolean.TRUE, redis.set(key, "jdk27", 60L).block(TIMEOUT));
            assertEquals("jdk27", redis.opsForValue().get(key).block(TIMEOUT));
        } finally {
            try {
                redis.delete(key).block(TIMEOUT);
                assertEquals(Boolean.FALSE, redis.hasKey(key).block(TIMEOUT));
            } finally { ((LettuceConnectionFactory) redis.getConnectionFactory()).destroy(); }
        }
    }

    @Test void mongoSingleNodeCrudAndCleanup() {
        String database = unique();
        var config = new AbstractReactiveMongoRepositoryConfig() {
            protected String host() { return InfrastructureIT.host() + ":27017"; }
            protected String user() { return InfrastructureIT.user(); }
            protected String password() { return InfrastructureIT.password(); }
            protected String authDatabase() { return "admin"; }
            protected String database() { return database; }
            protected Integer connectTimeoutSeconds() { return 5; }
            protected Integer maxIdleTimeSeconds() { return 30; }
            protected Integer maxSize() { return 4; }
            protected String replicaSetName() { return ""; }
            protected String deployModel() { return "single"; }
        };
        try (MongoClient client = config.buildMongoOperation()) {
            var db = client.getDatabase(database);
            var collection = db.getCollection("probe");
            try {
                Mono.from(collection.insertOne(new Document("_id", "jdk27").append("value", 27))).block(TIMEOUT);
                var found = Mono.from(collection.find(new Document("_id", "jdk27")).first()).block(TIMEOUT);
                assertNotNull(found);
                assertEquals(27, found.getInteger("value"));
                var updated = Mono.from(collection.updateOne(new Document("_id", "jdk27"),
                        new Document("$set", new Document("value", 28)))).block(TIMEOUT);
                assertEquals(1, updated.getModifiedCount());
                assertEquals(28, Mono.from(collection.find(new Document("_id", "jdk27")).first())
                        .block(TIMEOUT).getInteger("value"));
                assertEquals(1, Mono.from(collection.deleteOne(new Document("_id", "jdk27")))
                        .block(TIMEOUT).getDeletedCount());
            } finally {
                Mono.from(db.drop()).block(TIMEOUT);
                assertFalse(reactor.core.publisher.Flux.from(client.listDatabaseNames()).collectList().block(TIMEOUT).contains(database));
            }
        }
    }

    @Test void mongoBootContextWithoutReplicaProperty() {
        String database = unique();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataMongoReactiveAutoConfiguration.class))
                .withUserConfiguration(DefaultReactiveMongoRepositoryConfig.class)
                .withPropertyValues("lambda.repository.mongo.deploy=single",
                        "lambda.repository.mongo.host=" + host() + ":27017",
                        "lambda.repository.mongo.user=" + user(),
                        "lambda.repository.mongo.password=" + password(),
                        "lambda.repository.mongo.auth-database=admin",
                        "lambda.repository.mongo.database=" + database)
                .run(context -> {
                    assertNull(context.getStartupFailure(), "Mongo Spring context should start without a replica property");
                    var template = context.getBean(ReactiveMongoTemplate.class);
                    try {
                        template.insert(new Document("_id", "spring-boot-4"), "probe").block(TIMEOUT);
                        assertNotNull(template.findById("spring-boot-4", Document.class, "probe").block(TIMEOUT));
                    } finally {
                        template.getMongoDatabase().flatMap(db -> Mono.from(db.drop())).block(TIMEOUT);
                        var client = context.getBean(MongoClient.class);
                        assertFalse(reactor.core.publisher.Flux.from(client.listDatabaseNames())
                                .collectList().block(TIMEOUT).contains(database));
                    }
                });
    }

    @Test void rabbitDelayedMessageAndCleanup() throws Exception {
        var config = new AbstractReactiveRabbitMQConfig() {
            protected String host() { return InfrastructureIT.host() + ":5672"; }
            protected String user() { return InfrastructureIT.user(); }
            protected String password() { return InfrastructureIT.password(); }
        };
        var factory = config.connectionFactory();
        String exchange = unique(), queue = unique();
        try (Connection connection = factory.newConnection(new Address[]{new Address(host(), 5672)});
             Channel channel = connection.createChannel(); Sender sender = config.buildSender(factory)) {
            try {
                sender.declareExchange(ExchangeSpecification.exchange(exchange).type("x-delayed-message").arguments(Map.of("x-delayed-type", "direct"))).block(TIMEOUT);
                sender.declareQueue(QueueSpecification.queue(queue)).block(TIMEOUT);
                sender.bind(BindingSpecification.binding(exchange, "probe", queue)).block(TIMEOUT);
                var properties = new AMQP.BasicProperties.Builder().headers(Map.of("x-delay", 2000)).build();
                long start = System.nanoTime();
                var confirmed = sender.sendWithPublishConfirms(Mono.just(new OutboundMessage(exchange, "probe", properties, "jdk27".getBytes(java.nio.charset.StandardCharsets.UTF_8)))).blockLast(TIMEOUT);
                assertNotNull(confirmed); assertTrue(confirmed.isAck());
                assertNull(channel.basicGet(queue, true), "Delayed message arrived immediately");
                GetResponse received = null;
                long deadline = start + Duration.ofSeconds(10).toNanos();
                while (received == null && System.nanoTime() < deadline) {
                    Thread.sleep(100);
                    received = channel.basicGet(queue, true);
                }
                assertNotNull(received, "Delayed message did not arrive");
                assertTrue(System.nanoTime() - start >= Duration.ofMillis(1900).toNanos());
                assertEquals("jdk27", new String(received.getBody(), java.nio.charset.StandardCharsets.UTF_8));
            } finally {
                channel.queueDelete(queue);
                channel.exchangeDelete(exchange);
            }
        }
    }
}
