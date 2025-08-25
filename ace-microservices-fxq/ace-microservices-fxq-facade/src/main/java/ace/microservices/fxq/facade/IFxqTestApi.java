package ace.microservices.fxq.facade;

import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import reactor.core.publisher.Mono;

@RSocketRpcDiscorvery("ace-microservices-fxq")
public interface IFxqTestApi {
    @RSocketRpcType
    public Mono<String> test();
}
