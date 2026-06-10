package com.example.sqs.web.dto;

import com.example.sqs.model.QueueType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class QueueEditForm {

    @NotBlank
    private String queueUrl;

    @NotBlank
    private String name;

    private QueueType queueType = QueueType.STANDARD;

    @Min(0)
    private Integer visibilityTimeout = 30;

    @Min(0)
    private Integer messageRetentionPeriod = 345600;

    @Min(0)
    private Integer delaySeconds = 0;

    private Boolean contentBasedDeduplication;

    public String getQueueUrl() {
        return queueUrl;
    }

    public void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public boolean isFifo() {
        return queueType != null && queueType.isFifo();
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public Integer getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(Integer messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public Boolean getContentBasedDeduplication() {
        return contentBasedDeduplication;
    }

    public void setContentBasedDeduplication(Boolean contentBasedDeduplication) {
        this.contentBasedDeduplication = contentBasedDeduplication;
    }
}
