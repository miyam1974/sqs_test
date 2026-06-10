package com.example.sqs.web.dto;

import com.example.sqs.model.DeduplicationScopeOption;
import com.example.sqs.model.FifoThroughputLimitOption;
import com.example.sqs.model.QueueType;
import com.example.sqs.model.RedrivePermissionOption;
import com.example.sqs.model.SseModeOption;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QueueForm {

    @NotBlank(message = "キュー名は必須です")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "英数字・ハイフン・アンダースコアのみ（.fifo は自動付与）")
    private String name;

    @NotNull(message = "キュータイプを選択してください")
    private QueueType queueType = QueueType.STANDARD;

    @Min(0)
    @Max(900)
    private Integer delaySeconds = 0;

    @Min(1024)
    @Max(1048576)
    private Integer maximumMessageSize = 1048576;

    @Min(60)
    @Max(1209600)
    private Integer messageRetentionPeriod = 345600;

    @Min(0)
    @Max(20)
    private Integer receiveMessageWaitTimeSeconds = 0;

    @Min(0)
    @Max(43200)
    private Integer visibilityTimeout = 30;

    /** unset | true | false */
    private String contentBasedDeduplicationMode = "unset";

    private DeduplicationScopeOption deduplicationScope = DeduplicationScopeOption.NOT_SET;

    private FifoThroughputLimitOption fifoThroughputLimit = FifoThroughputLimitOption.NOT_SET;

    private boolean redriveEnabled;

    @Size(max = 512)
    private String deadLetterTargetArn;

    private String deadLetterQueueName;

    @Min(1)
    @Max(1000)
    private Integer maxReceiveCount = 10;

    private boolean redriveAllowEnabled;

    private RedrivePermissionOption redrivePermission = RedrivePermissionOption.ALLOW_ALL;

    @Size(max = 4096)
    private String sourceQueueArns;

    @NotNull
    private SseModeOption sseMode = SseModeOption.NONE;

    @Size(max = 256)
    private String kmsMasterKeyId;

    @Min(60)
    @Max(86400)
    private Integer kmsDataKeyReusePeriodSeconds = 300;

    @Size(max = 8192)
    private String policy;

    @Size(max = 2048)
    private String tagsText;

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

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public Integer getMaximumMessageSize() {
        return maximumMessageSize;
    }

    public void setMaximumMessageSize(Integer maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
    }

    public Integer getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(Integer messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    public Integer getReceiveMessageWaitTimeSeconds() {
        return receiveMessageWaitTimeSeconds;
    }

    public void setReceiveMessageWaitTimeSeconds(Integer receiveMessageWaitTimeSeconds) {
        this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public String getContentBasedDeduplicationMode() {
        return contentBasedDeduplicationMode;
    }

    public void setContentBasedDeduplicationMode(String contentBasedDeduplicationMode) {
        this.contentBasedDeduplicationMode = contentBasedDeduplicationMode;
    }

    public DeduplicationScopeOption getDeduplicationScope() {
        return deduplicationScope;
    }

    public void setDeduplicationScope(DeduplicationScopeOption deduplicationScope) {
        this.deduplicationScope = deduplicationScope;
    }

    public FifoThroughputLimitOption getFifoThroughputLimit() {
        return fifoThroughputLimit;
    }

    public void setFifoThroughputLimit(FifoThroughputLimitOption fifoThroughputLimit) {
        this.fifoThroughputLimit = fifoThroughputLimit;
    }

    public boolean isRedriveEnabled() {
        return redriveEnabled;
    }

    public void setRedriveEnabled(boolean redriveEnabled) {
        this.redriveEnabled = redriveEnabled;
    }

    public String getDeadLetterTargetArn() {
        return deadLetterTargetArn;
    }

    public void setDeadLetterTargetArn(String deadLetterTargetArn) {
        this.deadLetterTargetArn = deadLetterTargetArn;
    }

    public String getDeadLetterQueueName() {
        return deadLetterQueueName;
    }

    public void setDeadLetterQueueName(String deadLetterQueueName) {
        this.deadLetterQueueName = deadLetterQueueName;
    }

    public Integer getMaxReceiveCount() {
        return maxReceiveCount;
    }

    public void setMaxReceiveCount(Integer maxReceiveCount) {
        this.maxReceiveCount = maxReceiveCount;
    }

    public boolean isRedriveAllowEnabled() {
        return redriveAllowEnabled;
    }

    public void setRedriveAllowEnabled(boolean redriveAllowEnabled) {
        this.redriveAllowEnabled = redriveAllowEnabled;
    }

    public RedrivePermissionOption getRedrivePermission() {
        return redrivePermission;
    }

    public void setRedrivePermission(RedrivePermissionOption redrivePermission) {
        this.redrivePermission = redrivePermission;
    }

    public String getSourceQueueArns() {
        return sourceQueueArns;
    }

    public void setSourceQueueArns(String sourceQueueArns) {
        this.sourceQueueArns = sourceQueueArns;
    }

    public SseModeOption getSseMode() {
        return sseMode;
    }

    public void setSseMode(SseModeOption sseMode) {
        this.sseMode = sseMode;
    }

    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    public void setKmsMasterKeyId(String kmsMasterKeyId) {
        this.kmsMasterKeyId = kmsMasterKeyId;
    }

    public Integer getKmsDataKeyReusePeriodSeconds() {
        return kmsDataKeyReusePeriodSeconds;
    }

    public void setKmsDataKeyReusePeriodSeconds(Integer kmsDataKeyReusePeriodSeconds) {
        this.kmsDataKeyReusePeriodSeconds = kmsDataKeyReusePeriodSeconds;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getTagsText() {
        return tagsText;
    }

    public void setTagsText(String tagsText) {
        this.tagsText = tagsText;
    }

    public String resolvedQueueName() {
        if (isFifo() && !name.endsWith(".fifo")) {
            return name + ".fifo";
        }
        return name;
    }

    @AssertTrue(message = "DLQ を有効にする場合、DLQ ARN または DLQ キュー名を指定してください")
    public boolean isRedriveConfigValid() {
        if (!redriveEnabled) {
            return true;
        }
        boolean hasArn = deadLetterTargetArn != null && !deadLetterTargetArn.isBlank();
        boolean hasName = deadLetterQueueName != null && !deadLetterQueueName.isBlank();
        return hasArn || hasName;
    }

    @AssertTrue(message = "redrivePermission=byQueue の場合、ソースキュー ARN を指定してください")
    public boolean isRedriveAllowConfigValid() {
        if (!redriveAllowEnabled) {
            return true;
        }
        if (redrivePermission != RedrivePermissionOption.BY_QUEUE) {
            return true;
        }
        return sourceQueueArns != null && !sourceQueueArns.isBlank();
    }

    @AssertTrue(message = "SSE-KMS を選択した場合、KMS マスターキー ID を指定してください")
    public boolean isKmsConfigValid() {
        if (sseMode != SseModeOption.KMS) {
            return true;
        }
        return kmsMasterKeyId != null && !kmsMasterKeyId.isBlank();
    }

    @AssertTrue(message = "perMessageGroupId は deduplicationScope=messageGroup のときのみ指定できます")
    public boolean isFifoThroughputConfigValid() {
        if (!isFifo() || fifoThroughputLimit != FifoThroughputLimitOption.PER_MESSAGE_GROUP_ID) {
            return true;
        }
        return deduplicationScope == DeduplicationScopeOption.MESSAGE_GROUP;
    }
}
