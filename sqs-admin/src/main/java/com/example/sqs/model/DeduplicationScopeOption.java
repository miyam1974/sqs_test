package com.example.sqs.model;

public enum DeduplicationScopeOption {
    NOT_SET("", "未設定（デフォルト）"),
    MESSAGE_GROUP("messageGroup", "メッセージグループ"),
    QUEUE("queue", "キュー");

    private final String apiValue;
    private final String label;

    DeduplicationScopeOption(String apiValue, String label) {
        this.apiValue = apiValue;
        this.label = label;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSet() {
        return apiValue != null && !apiValue.isBlank();
    }
}
