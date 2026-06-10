package com.example.sqs.model;

public enum SseModeOption {
    NONE("none", "なし"),
    SQS_MANAGED("sqsManaged", "SQS マネージド SSE"),
    KMS("kms", "KMS (SSE-KMS)");

    private final String value;
    private final String label;

    SseModeOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
