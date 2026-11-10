package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.contract.SystemContract;
import com.lambda.example.servicea.service.PartnerStatusServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@ConditionalOnProperty("example.partner.base-url")
public class PartnerStatusController {
    @Resource
    private PartnerStatusServiceImpl partner;

    @MessageMapping("/api/partner/status")
    public Mono<SystemContract.PartnerStatus> status(@Payload SystemContract.EmptyRequest input) {
        return partner.status(input);
    }
}
