package ace.microservices.service.server.mvc.api;
import ace.microservices.service.facade.ICustLoanApi;
import ace.microservices.service.facade.po.CustLoanPO;
import ace.microservices.service.server.mvc.function.CustLoanFunction;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcApi;
import org.lambda.framework.repository.operation.Paged;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RSocketRpcApi
public class CustLoanApi implements ICustLoanApi {

    @Resource
    private CustLoanFunction custLoanFunction;


    @Override
    public Mono<Void> put(PutDTO dto) {
        return custLoanFunction.put(dto);
    }

    @Override
    public Flux<CustLoanPO> find(FindDTO dto) {
        return custLoanFunction.find(dto);
    }

    @Override
    public Mono<CustLoanPO> get(GetDTO dto) {
        return custLoanFunction.get(dto);
    }

    @Override
    public Mono<Paged<CustLoanPO.AggregationDetails>> pageDetails(PageDetailsDTO dto) {
        return custLoanFunction.pageDetails(dto);
    }

    @Override
    public Mono<Paged<CustLoanPO>> page(PageDTO dto) {
        return custLoanFunction.page(dto);
    }
}
