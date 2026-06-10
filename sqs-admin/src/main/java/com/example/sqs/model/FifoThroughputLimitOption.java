package com.example.sqs.model;

public enum FifoThroughputLimitOption {
    NOT_SET("", "未設定（デフォルト）"),
    PER_QUEUE("perQueue", "キュー単位"),
    PER_MESSAGE_GROUP_ID("perMessageGroupId", "メッセージグループ単位");

    private final String apiValue;
    private final String label;

    FifoThroughputLimitOption(String apiValue, String label) {
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
