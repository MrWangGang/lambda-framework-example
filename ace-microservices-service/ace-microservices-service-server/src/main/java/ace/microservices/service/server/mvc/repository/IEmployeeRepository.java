package ace.microservices.service.server.mvc.repository;

import ace.microservices.service.facade.po.EmployeePO;
import org.lambda.framework.repository.operation.mangodb.ReactiveMongoCrudRepositoryOperation;
import org.springframework.stereotype.Repository;

@Repository
public interface IEmployeeRepository extends ReactiveMongoCrudRepositoryOperation<EmployeePO,String> {

}
