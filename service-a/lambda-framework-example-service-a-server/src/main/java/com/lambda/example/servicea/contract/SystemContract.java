package com.lambda.example.servicea.contract;

public final class SystemContract {
    private SystemContract() {}

    public record EmptyRequest() {}
    public record Info(String application, String greeting, String replicaSet) {}
    public record PartnerStatus(String partnerStatus) {}
}
