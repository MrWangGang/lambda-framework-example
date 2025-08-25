package ace.microservices.service.facade;
import ace.microservices.service.facade.po.CustLoanPO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import org.lambda.framework.repository.operation.Paged;
import org.lambda.framework.repository.operation.Paging;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RSocketRpcDiscorvery("ace-microservices-service")
public interface ICustLoanApi {
    @RSocketRpcType
    public Mono<Void> put(PutDTO dto);
    @RSocketRpcType
    public Flux<CustLoanPO> find(FindDTO dto);
    @RSocketRpcType
    public Mono<CustLoanPO> get(GetDTO dto);
    @RSocketRpcType
    public Mono<Paged<CustLoanPO.AggregationDetails>> pageDetails(PageDetailsDTO dto);
    @RSocketRpcType
    public Mono<Paged<CustLoanPO>> page(PageDTO dto);



    @Data
    @SuperBuilder
    @AllArgsConstructor
    public static class PutDTO extends CustLoanPO {

    }

    @Data
    @SuperBuilder
    @AllArgsConstructor
    public static class FindDTO extends CustLoanPO {

    }

    @Data
    @SuperBuilder
    @AllArgsConstructor
    public static class GetDTO extends CustLoanPO {

    }


    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDetailsDTO extends Paging {
        private String custNo;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDTO extends Paging {
        private String custNo;
    }

}
