package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.service.CatalogMaintenanceService;
import com.lambda.example.servicea.service.CatalogMaintenanceService.LockProvider;
import com.lambda.example.servicea.service.CatalogMaintenanceService.RebuildResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class CatalogMaintenanceController {
    private final CatalogMaintenanceService maintenance;

    public CatalogMaintenanceController(CatalogMaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @MessageMapping("/api/catalog/maintenance/rebuild")
    public Mono<RebuildResult> rebuild(@Valid @Payload RebuildRequest input) {
        return maintenance.rebuild(input.category(), input.lockProvider());
    }

    public record RebuildRequest(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String category,
                                 @NotNull LockProvider lockProvider) {}
}
