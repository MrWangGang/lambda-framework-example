package ace.microservices.service.server.mvc.api;
import ace.microservices.service.facade.IEmployeeApi;
import ace.microservices.service.server.mvc.function.EmployeeFunction;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcApi;
import reactor.core.publisher.Mono;

@RSocketRpcApi
public class EmployeeApi implements IEmployeeApi {

    @Resource
    private EmployeeFunction employeeFunction;


    @Override
    public Mono<Void> register(RegisterDTO dto) {
        return employeeFunction.register(dto);
    }

    @Override
    public Mono<String> login(LoginDTO dto) {
        return employeeFunction.login(dto);
    }
}
