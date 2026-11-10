package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class CatalogMaintenanceContract {
    private CatalogMaintenanceContract() {}

    public enum LockProvider { REDIS, ZOOKEEPER }
    public record RebuildRequest(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String category,
                                 @NotNull LockProvider lockProvider) {}
    public record RebuildResult(String jobId, String productCount) {}
}
