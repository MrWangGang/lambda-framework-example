package ace.microservices.fxq.server.mvc.controller;

import ace.microservices.fxq.server.mvc.function.CustFunction;
import ace.microservices.service.facade.ICustLoanApi;
import ace.microservices.service.facade.po.CustLoanPO;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ace.microservices.fxq.server.constant.Route.*;

@Controller
public class CustController {
    @Resource
    private CustFunction custFunction;

    @MessageMapping(CUST_PUT)
    public Mono<Void> put(ICustLoanApi.PutDTO dto){
        return custFunction.put(dto);
    }

    @MessageMapping(CUST_FIND)
    public Flux<CustLoanPO> find(ICustLoanApi.FindDTO dto){
        return custFunction.find(dto);
    }

    @MessageMapping(CUST_GET)
    public Mono<CustLoanPO> get(ICustLoanApi.GetDTO dto){
        return custFunction.get(dto);
    }

    @MessageMapping(CUST_PAGEDETAILS)
    public Mono<Paged<CustLoanPO.AggregationDetails>> pageDetails(ICustLoanApi.PageDetailsDTO dto){
        return custFunction.pageDetails(dto);
    }

    @MessageMapping(CUST_PAGE)
    public Mono<Paged<CustLoanPO>> page(ICustLoanApi.PageDTO dto){
        return custFunction.page(dto);
    }

}
