package ace.microservices.service.server.config;

import com.mongodb.reactivestreams.client.MongoClient;
import org.lambda.framework.repository.config.mongodb.DefaultReactiveMongoRepositoryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig extends DefaultReactiveMongoRepositoryConfig {

}
