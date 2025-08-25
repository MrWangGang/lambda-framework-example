package ace.microservices.service.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"ace.microservices","org.lambda.framework"} )
public class Run {
    public static void main(String[] env) {
        // 获取类上的注解
        SpringApplication.run(Run.class, env);
    }
}


