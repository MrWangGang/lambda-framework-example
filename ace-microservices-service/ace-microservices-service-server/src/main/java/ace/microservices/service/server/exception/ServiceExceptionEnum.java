package ace.microservices.service.server.exception;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum ServiceExceptionEnum implements ExceptionEnumFunction {
    EB_SERVICE_000("EB_SERVICE_000","DTO对象必须存在");

    private final String code;

    private final String message;
    // 构造方法
    ServiceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
