package ace.microservices.fxq.server.mvc.function;

import ace.microservices.fxq.server.mvc.api.FxqTestApi;
import ace.microservices.service.facade.po.EmployeePO;
import jakarta.annotation.Resource;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@Transactional
public class FxqTestFunction {
    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;

    public Mono<String> test() {
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class)
                .flatMap(emp->{
                    return Mono.just("fxq: " + emp.getName());
                });
    }
}
