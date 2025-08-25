package ace.microservices.service.server.mvc.api;

import ace.microservices.service.facade.IServiceTestApi;
import ace.microservices.service.server.mvc.function.ServiceTestFuncion;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcApi;
import reactor.core.publisher.Mono;

@RSocketRpcApi
public class ServiceTestApi implements IServiceTestApi {
    @Resource
    private ServiceTestFuncion serviceTestFuncion;
    @Override
    public Mono<String> test() {
        return serviceTestFuncion.test();
    }
}
