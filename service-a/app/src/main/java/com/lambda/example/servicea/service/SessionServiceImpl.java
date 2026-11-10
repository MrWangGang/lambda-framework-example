package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ExampleError;
import org.lambda.framework.common.exception.BusinessException;
import com.lambda.example.servicea.contract.SessionContract;
import com.lambda.example.servicea.contract.SystemContract;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@Validated
public class SessionServiceImpl {
    @Resource
    private PrincipalFactory principals;
    @Value("${example.demo-password}")
    private String configuredPassword;
    @Value("${example.demo-user:demo}")
    private String demoUser;
    private byte[] demoPassword;

    @PostConstruct
    void initialize() {
        if (configuredPassword.isBlank() || !demoUser.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Configure a demo login password");
        }
        demoPassword = configuredPassword.getBytes(StandardCharsets.UTF_8);
    }

    public Mono<SessionContract.LoginReply> login(@Valid @NotNull SessionContract.LoginRequest request) {
        if (!demoUser.equals(request.username()) || !MessageDigest.isEqual(
                demoPassword, request.password().getBytes(StandardCharsets.UTF_8))) {
            return Mono.error(new BusinessException(ExampleError.LOGIN_FAILED));
        }
        var user = new DemoUser();
        user.setId(demoUser);
        return principals.setPrincipal(user).map(SessionContract.LoginReply::new);
    }

    public Mono<SessionContract.UserInfo> whoAmI(SystemContract.EmptyRequest input) {
        return ReactiveValues.require(principals.currentUserId(), () -> new BusinessException(ExampleError.SESSION_MISSING))
                .map(SessionContract.UserInfo::new);
    }

    public Mono<SessionContract.UserInfo> profile(SystemContract.EmptyRequest input) {
        return ReactiveValues.require(principals.currentUser(DemoUser.class), () -> new BusinessException(ExampleError.SESSION_MISSING))
                .map(user -> new SessionContract.UserInfo(user.getId()));
    }

    public Mono<Void> logout(SystemContract.EmptyRequest input) {
        return principals.deletePrincipal();
    }

    @Getter
    @Setter
    public static class DemoUser implements SecurityLoginUser<String> {
        private String id;
    }
}
