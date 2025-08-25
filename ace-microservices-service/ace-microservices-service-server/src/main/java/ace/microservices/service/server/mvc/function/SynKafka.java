package ace.microservices.service.server.mvc.function;

import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.mq.config.kafka.KafkaMQReactiveOperate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static ace.microservices.fxq.facade.constant.Constant.MQ_KAFKA_FXQ_TEST;

@Component
public class SynKafka {
    @Resource
    private KafkaMQReactiveOperate kafkaMQReactiveOperate;

    @EventListener(ApplicationReadyEvent.class)
    public void mq(){
        //自动
        kafkaMQReactiveOperate.receiverAuto(MQ_KAFKA_FXQ_TEST).subscribe(e->{
             e.subscribe(x->{
                System.out.println(x.key());
                System.out.println(x.value());
            });
        });

        //手动
        //kafkaMQReactiveOperate.receiverManual(MQ_KAFKA_FXQ_TEST).subscribe(e->{
        //    System.out.println(x.key());
        //    System.out.println(x.value());
        //    e.receiverOffset().acknowledge();
        //});

    }
}
