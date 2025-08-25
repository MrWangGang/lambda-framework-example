package ace.microservices.service.facade.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document("t_cust_loan")
public class CustLoanPO extends UnifyPO<String> {
    @Indexed(background = true, unique = true)
    private String custNo;
    private String custName;
    private String totalAmount;
    private LoanDetail detail;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanDetail{
        @Indexed(background = true, unique = true)
        private String month;
        private String amount;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationDetails {
        private String id;
        private String custNo;
        private String month;
        private String amount;
    }
}
