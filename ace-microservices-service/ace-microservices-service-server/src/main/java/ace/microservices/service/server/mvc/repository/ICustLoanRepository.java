package ace.microservices.service.server.mvc.repository;

import ace.microservices.service.facade.po.CustLoanPO;
import org.lambda.framework.repository.operation.mangodb.ReactiveMongoCrudRepositoryOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ICustLoanRepository extends ReactiveMongoCrudRepositoryOperation<CustLoanPO,String> {
    @Aggregation(pipeline = {
            "{ '$match': { 'custNo': ?0 } }",
            "{ '$match': { 'detail.amount': { '$ne': null } } }",
            "{ '$addFields': { 'detail.amount_double': { '$toDouble': '$detail.amount' } } }",
            "{ '$match': { 'detail.amount_double': { '$gt': 10000 } } }",
            "{ '$project': { 'id': '$_id', 'custNo': '$custNo', 'month': '$detail.month', 'amount': '$detail.amount' } }"
    })
    Flux<CustLoanPO.AggregationDetails> pageDetails(String custNo,Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'custNo': ?0 } }",
            "{ '$match': { 'detail.amount': { '$ne': null } } }",
            "{ '$addFields': { 'detail.amount_double': { '$toDouble': '$detail.amount' } } }",
            "{ '$match': { 'detail.amount_double': { '$gt': 10000 } } }",
            "{ '$count': 'totalCount' }"
    })
    Mono<Long> countDetails(String custNo);
}
