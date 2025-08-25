package ace.microservices.fxq.server.mvc.function;

import ace.microservices.service.facade.IEmployeeApi;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@Transactional
public class LoginFunction {
    @RSocketRpc
    private IEmployeeApi iEmployeeApi;

    public Mono<Void> register(IEmployeeApi.RegisterDTO dto){
        return iEmployeeApi.register(dto);
    }

    public Mono<String> login(IEmployeeApi.LoginDTO dto){
        return iEmployeeApi.login(dto);
    }
}
