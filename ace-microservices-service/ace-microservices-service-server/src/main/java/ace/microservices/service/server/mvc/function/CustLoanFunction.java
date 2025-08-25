package ace.microservices.service.server.mvc.function;
import ace.microservices.service.facade.ICustLoanApi;
import ace.microservices.service.facade.po.CustLoanPO;
import ace.microservices.service.facade.po.EmployeePO;
import ace.microservices.service.server.mvc.repository.ICustLoanRepository;
import ace.microservices.service.server.mvc.service.ICustLoanService;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.lambda.framework.repository.operation.Paged;
import org.lambda.framework.repository.operation.UnifyPagingSqlOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static ace.microservices.service.server.exception.ServiceExceptionEnum.EB_SERVICE_000;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@Component
@Transactional
public class CustLoanFunction {
    @Resource
    private ICustLoanService iCustLoanService;

    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;

    public Mono<Void> put(ICustLoanApi.PutDTO dto){
        Assert.check(dto,EB_SERVICE_000,"对象不能为空");
        Assert.verify(dto.getCustNo(),EB_SERVICE_000,"客户号不能为空");
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class).flatMap(emp->{
            if("admin".equals(emp.getName())){
                return iCustLoanService.insert(dto).then();
            }
            throw new EventException(EB_SERVICE_000,"只有管理员可以操作");
        });
    }

    public Flux<CustLoanPO> find(ICustLoanApi.FindDTO dto){
        Assert.check(dto,EB_SERVICE_000,"对象不能为空");
        Assert.verify(dto.getCustNo(),EB_SERVICE_000,"客户号不能为空");
        return iCustLoanService.find(dto);
    }

    public Mono<CustLoanPO> get(ICustLoanApi.GetDTO dto){
        Assert.check(dto,EB_SERVICE_000,"对象不能为空");
        Assert.verify(dto.getCustNo(),EB_SERVICE_000,"客户号不能为空");
        return iCustLoanService.get(dto);
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public Mono<Paged<CustLoanPO.AggregationDetails>> pageDetails(ICustLoanApi.PageDetailsDTO dto) {
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class).flatMap(emp->{
            return iCustLoanService.find(dto, dto, Sort.by(Sort.Direction.DESC, "createTime"),new UnifyPagingSqlOperation<ICustLoanApi.PageDetailsDTO,CustLoanPO.AggregationDetails>() {
                @Override
                public Flux<CustLoanPO.AggregationDetails> query(ICustLoanApi.PageDetailsDTO paging, Pageable pageable) {
                    return iCustLoanService.<ICustLoanRepository>repository().pageDetails(paging.getCustNo(),pageable);
                }

                @Override
                public Mono<Long> count(ICustLoanApi.PageDetailsDTO paging) {
                    return iCustLoanService.<ICustLoanRepository>repository().countDetails(paging.getCustNo());
                }
            });
        });
    }


    @Transactional(propagation = NOT_SUPPORTED)
    public Mono<Paged<CustLoanPO>> page(ICustLoanApi.PageDTO dto) {
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class).flatMap(emp->{
            CustLoanPO po = CustLoanPO.builder().custNo(dto.getCustNo()).build();
            return iCustLoanService.find(dto,po,Sort.by(Sort.Direction.DESC, "createTime"));
        });
    }

}
