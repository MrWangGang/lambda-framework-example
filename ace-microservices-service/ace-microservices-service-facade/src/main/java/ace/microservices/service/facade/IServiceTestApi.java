package ace.microservices.service.facade;

import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import reactor.core.publisher.Mono;

@RSocketRpcDiscorvery("ace-microservices-service")
public interface IServiceTestApi {
    @RSocketRpcType
    public Mono<String> test();
}
