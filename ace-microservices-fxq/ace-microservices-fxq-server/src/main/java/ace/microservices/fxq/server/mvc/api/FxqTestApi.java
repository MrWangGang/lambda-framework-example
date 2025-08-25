package ace.microservices.fxq.server.mvc.api;

import ace.microservices.fxq.facade.IFxqTestApi;
import ace.microservices.fxq.server.mvc.function.FxqTestFunction;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcApi;
import reactor.core.publisher.Mono;

@RSocketRpcApi
public class FxqTestApi implements IFxqTestApi {
    @Resource
    private FxqTestFunction fxqTestFunction;

    @Override
    public Mono<String> test() {
        return fxqTestFunction.test();
    }
}
