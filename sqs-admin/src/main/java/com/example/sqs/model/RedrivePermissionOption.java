package com.example.sqs.model;

public enum RedrivePermissionOption {
    ALLOW_ALL("allowAll", "すべて許可"),
    DENY_ALL("denyAll", "すべて拒否"),
    BY_QUEUE("byQueue", "指定キューのみ");

    private final String apiValue;
    private final String label;

    RedrivePermissionOption(String apiValue, String label) {
        this.apiValue = apiValue;
        this.label = label;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getLabel() {
        return label;
    }
}
