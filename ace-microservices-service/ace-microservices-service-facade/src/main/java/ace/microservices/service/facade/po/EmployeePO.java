package ace.microservices.service.facade.po;

import ace.microservices.service.facade.compact.EnumEmployeeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document("t_employee")
public class EmployeePO  extends UnifyPO<String> implements SecurityLoginUser<String> {
    private String name;
    //柜员号
    @Indexed(background = true, unique = true)
    private String no;
    //账户
    @Indexed(background = true, unique = true)
    private String account;

    private String password;

    private String phone;

    private EnumEmployeeType type;
}
