package ace.microservices.service.facade.compact;

import lombok.Getter;
import org.lambda.framework.common.enums.ConverterEnum;
@Getter
public enum EnumEmployeeType implements ConverterEnum {
    ENUM_EMPLOYEE_TYPE_ADMIN("管理员","ADMIN"),
    ENUM_EMPLOYEE_TYPE_NORMAL("普通用户","NORMAL");

    EnumEmployeeType(String description, String value) {
        this.description = description;
        this.value = value;
    }

    private final String value;
    private final String description;
}
