package ace.microservices.service.server.mvc.function;

import ace.microservices.fxq.facade.IFxqTestApi;
import ace.microservices.service.facade.po.EmployeePO;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
@Component
@Transactional
public class ServiceTestFuncion {
    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;
    @RSocketRpc
    private IFxqTestApi iFxqTestApi;
    public Mono<String> test() {
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class)
                        .flatMap(emp->{
                            return Mono.zip(Mono.just("service: "+emp.getName()),iFxqTestApi.test())
                                    .flatMap(tuple->{
                                       String service_ehco = tuple.getT1();
                                       String fxq_ehco = tuple.getT2();
                                       return Mono.just(service_ehco +"---"+ fxq_ehco);
                                    });
                        });
    }
}
