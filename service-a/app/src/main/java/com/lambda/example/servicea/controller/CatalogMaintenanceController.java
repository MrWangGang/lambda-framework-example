package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.contract.CatalogMaintenanceContract;
import com.lambda.example.servicea.service.CatalogMaintenanceServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class CatalogMaintenanceController {
    @Resource
    private CatalogMaintenanceServiceImpl maintenance;

    @MessageMapping("/api/catalog/maintenance/rebuild")
    public Mono<CatalogMaintenanceContract.RebuildResult> rebuild(@Payload CatalogMaintenanceContract.RebuildRequest input) {
        return maintenance.rebuild(input);
    }
}
