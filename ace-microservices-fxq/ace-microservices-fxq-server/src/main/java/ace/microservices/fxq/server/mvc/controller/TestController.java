package ace.microservices.fxq.server.mvc.controller;

import ace.microservices.service.facade.IServiceTestApi;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.lock.ZookeeperBuilder;
import org.lambda.framework.lock.ZookeeperLock;
import org.lambda.framework.mq.config.kafka.KafkaMQReactiveOperate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import static ace.microservices.fxq.facade.constant.Constant.FXQ_LOCK;
import static ace.microservices.fxq.facade.constant.Constant.MQ_KAFKA_FXQ_TEST;
import static ace.microservices.fxq.server.constant.Route.*;
import static ace.microservices.fxq.server.exception.FxqExceptionEnum.EB_FXQ_000;

@Controller
public class TestController {
    @RSocketRpc
    private IServiceTestApi iServiceTestApi;

    @MessageMapping(TEST_MESSAGE)
    public Mono<String> test(){
        return iServiceTestApi.test();
    }

    @Resource
    private KafkaMQReactiveOperate kafkaMQReactiveOperate;

    @Resource
    private ZookeeperBuilder zookeeperBuilder;



    @MessageMapping(PUSH_KAFKA)
    public Mono<Void> pushKafka(){
        return kafkaMQReactiveOperate.send(MQ_KAFKA_FXQ_TEST,"test","message :kafka test");
    }


    @MessageMapping(TEST_ZK)
    public Mono<Void> testZookeeper(){
        ZookeeperLock zookeeperLock = zookeeperBuilder.getLock(FXQ_LOCK);
        //if(zookeeperLock.tryLock()){
        //    zookeeperLock.unlock();
        //}

        if(!zookeeperLock.tryLock()){
            throw new EventException(EB_FXQ_000,"获取锁失败");
        }
        return Mono.empty();
    }
}
