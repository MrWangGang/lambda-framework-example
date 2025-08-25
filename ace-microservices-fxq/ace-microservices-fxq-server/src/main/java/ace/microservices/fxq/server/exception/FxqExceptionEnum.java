package ace.microservices.fxq.server.exception;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum FxqExceptionEnum implements ExceptionEnumFunction {
    EB_FXQ_000("EB_FXQ_000","DTO对象必须存在");

    private final String code;

    private final String message;
    // 构造方法
    FxqExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
