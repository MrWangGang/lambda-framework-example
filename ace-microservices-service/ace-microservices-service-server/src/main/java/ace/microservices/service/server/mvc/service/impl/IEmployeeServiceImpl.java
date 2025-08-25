package ace.microservices.service.server.mvc.service.impl;

import ace.microservices.service.facade.po.EmployeePO;
import ace.microservices.service.server.mvc.repository.IEmployeeRepository;
import ace.microservices.service.server.mvc.service.IEmployeeService;
import org.lambda.framework.compliance.service.impl.DefaultBasicServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class IEmployeeServiceImpl extends DefaultBasicServiceImpl<EmployeePO,String, IEmployeeRepository> implements IEmployeeService {
    public IEmployeeServiceImpl(IEmployeeRepository repository) {
        super(repository);
    }
}
