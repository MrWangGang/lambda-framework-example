package ace.microservices.service.facade;

import ace.microservices.service.facade.compact.EnumEmployeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import reactor.core.publisher.Mono;

@RSocketRpcDiscorvery("ace-microservices-service")
public interface IEmployeeApi {
    @RSocketRpcType
    public Mono<Void> register(RegisterDTO dto);
    @RSocketRpcType
    public Mono<String> login(LoginDTO dto);


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginDTO {
        private String account;
        private String password;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterDTO {
        private String account;
        private String password;
        private String name;
        private String no;
        private String phone;
        private EnumEmployeeType type;
    }
}
