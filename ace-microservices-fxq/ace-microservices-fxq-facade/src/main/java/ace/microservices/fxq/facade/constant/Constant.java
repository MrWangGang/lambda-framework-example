package ace.microservices.fxq.facade.constant;

import org.lambda.framework.common.mq.KafkaMQDeclare;

public class Constant {
    private final static String KAFKA_FXQ_TEST = "fxq.test";
    public final static KafkaMQDeclare MQ_KAFKA_FXQ_TEST = KafkaMQDeclare.builder()
            .groupId(KAFKA_FXQ_TEST)
            .topic(KAFKA_FXQ_TEST)
            .build();

    public final static String FXQ_LOCK = "/_fxq/test/_wr/lock";

}
