package ace.microservices.fxq.server.mvc.function;
import ace.microservices.service.facade.ICustLoanApi;
import ace.microservices.service.facade.po.CustLoanPO;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Component
@Transactional
public class CustFunction {
    @RSocketRpc
    private ICustLoanApi iCustLoanApi;

    public Mono<Void> put(ICustLoanApi.PutDTO dto){
        return iCustLoanApi.put(dto);
    }

    public Flux<CustLoanPO> find(ICustLoanApi.FindDTO dto){
        return iCustLoanApi.find(dto);
    }

    public Mono<CustLoanPO> get(ICustLoanApi.GetDTO dto){
        return iCustLoanApi.get(dto);
    }

    public Mono<Paged<CustLoanPO.AggregationDetails>> pageDetails(ICustLoanApi.PageDetailsDTO dto){
        return iCustLoanApi.pageDetails(dto);
    }

    public Mono<Paged<CustLoanPO>> page(ICustLoanApi.PageDTO dto){
        return iCustLoanApi.page(dto);
    }

}
