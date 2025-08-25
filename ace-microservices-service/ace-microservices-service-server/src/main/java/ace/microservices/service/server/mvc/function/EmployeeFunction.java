package ace.microservices.service.server.mvc.function;

import ace.microservices.service.facade.IEmployeeApi;
import ace.microservices.service.facade.po.EmployeePO;
import ace.microservices.service.server.mvc.service.IEmployeeService;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import static ace.microservices.service.server.exception.ServiceExceptionEnum.EB_SERVICE_000;

@Component
@Transactional
public class EmployeeFunction {
    @Resource
    private IEmployeeService iEmployeeService;

    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;

    public Mono<String> login(IEmployeeApi.LoginDTO dto){
        Assert.check(dto,EB_SERVICE_000,"用户名和密码不能为空");

        EmployeePO loginForm = EmployeePO.builder()
                .account(dto.getAccount())
                .password(dto.getPassword())
                .build();

        return iEmployeeService.get(loginForm)
                .switchIfEmpty(Mono.error(new EventException(EB_SERVICE_000,"该用户不存在")))
                .flatMap(employee -> {
                   return securityPrincipalHolder.setPrincipal(employee);
                });
    }


    public Mono<Void> register(IEmployeeApi.RegisterDTO dto){
        Assert.check(dto,EB_SERVICE_000,"用户信息不能为空");

        EmployeePO registerForm = EmployeePO.builder()
                .account(dto.getAccount())
                .password(dto.getPassword())
                .no(dto.getNo())
                .name(dto.getName())
                .phone(dto.getPhone())
                .type(dto.getType())
                .build();

        return iEmployeeService.insert(registerForm).then();
    }

}
