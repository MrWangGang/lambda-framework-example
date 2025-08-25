package ace.microservices.fxq.server.mvc.controller;

import ace.microservices.fxq.server.mvc.function.LoginFunction;
import ace.microservices.service.facade.IEmployeeApi;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import static ace.microservices.fxq.server.constant.Route.EMPLOYEE_LOGIN;
import static ace.microservices.fxq.server.constant.Route.EMPLOYEE_REGISTER;

@Controller
public class LoginController {
    @Resource
    private LoginFunction loginFunction;

    @MessageMapping(EMPLOYEE_LOGIN)
    public Mono<String> login(IEmployeeApi.LoginDTO dto) {
        return loginFunction.login(dto);
    }

    @MessageMapping(EMPLOYEE_REGISTER)
    public Mono<Void> register(IEmployeeApi.RegisterDTO dto) {
        return loginFunction.register(dto);
    }
}
