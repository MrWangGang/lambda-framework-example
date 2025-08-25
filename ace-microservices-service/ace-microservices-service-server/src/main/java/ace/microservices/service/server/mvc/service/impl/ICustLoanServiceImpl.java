package ace.microservices.service.server.mvc.service.impl;

import ace.microservices.service.server.mvc.repository.ICustLoanRepository;
import ace.microservices.service.facade.po.CustLoanPO;
import ace.microservices.service.server.mvc.service.ICustLoanService;
import org.lambda.framework.compliance.service.impl.DefaultBasicServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ICustLoanServiceImpl extends DefaultBasicServiceImpl<CustLoanPO,String, ICustLoanRepository> implements ICustLoanService {
    public ICustLoanServiceImpl(ICustLoanRepository repository) {
        super(repository);
    }
}
