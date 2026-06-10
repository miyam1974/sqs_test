package com.example.sqs.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SendMessageForm {

    @NotBlank(message = "キューを選択してください")
    private String queueUrl;

    @NotBlank(message = "メッセージ本文は必須です")
    private String body;

    @Min(0)
    private Integer delaySeconds = 0;

    private String messageGroupId;

    private String messageDeduplicationId;

    public String getQueueUrl() {
        return queueUrl;
    }

    public void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }

    public void setMessageGroupId(String messageGroupId) {
        this.messageGroupId = messageGroupId;
    }

    public String getMessageDeduplicationId() {
        return messageDeduplicationId;
    }

    public void setMessageDeduplicationId(String messageDeduplicationId) {
        this.messageDeduplicationId = messageDeduplicationId;
    }
}
