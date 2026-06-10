package com.example.sqs.service;

import com.example.sqs.config.AwsSqsProperties;
import com.example.sqs.model.RedrivePermissionOption;
import com.example.sqs.model.SseModeOption;
import com.example.sqs.web.dto.QueueForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QueueAttributeBuilder {

    private final AwsSqsProperties awsSqsProperties;
    private final ObjectMapper objectMapper;

    public QueueAttributeBuilder(AwsSqsProperties awsSqsProperties, ObjectMapper objectMapper) {
        this.awsSqsProperties = awsSqsProperties;
        this.objectMapper = objectMapper;
    }

    public Map<QueueAttributeName, String> buildCreateAttributes(QueueForm form, String deadLetterArn) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        boolean fifo = form.isFifo();

        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(form.getVisibilityTimeout()));
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, String.valueOf(form.getMessageRetentionPeriod()));
        attributes.put(QueueAttributeName.MAXIMUM_MESSAGE_SIZE, String.valueOf(form.getMaximumMessageSize()));
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS,
                String.valueOf(form.getReceiveMessageWaitTimeSeconds()));
        attributes.put(QueueAttributeName.DELAY_SECONDS, String.valueOf(form.getDelaySeconds()));

        if (fifo) {
            attributes.put(QueueAttributeName.FIFO_QUEUE, "true");
            String dedupMode = form.getContentBasedDeduplicationMode();
            if ("true".equals(dedupMode)) {
                attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true");
            } else if ("false".equals(dedupMode)) {
                attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false");
            }
            if (form.getDeduplicationScope() != null && form.getDeduplicationScope().isSet()) {
                attributes.put(QueueAttributeName.DEDUPLICATION_SCOPE, form.getDeduplicationScope().getApiValue());
            }
            if (form.getFifoThroughputLimit() != null && form.getFifoThroughputLimit().isSet()) {
                attributes.put(QueueAttributeName.FIFO_THROUGHPUT_LIMIT, form.getFifoThroughputLimit().getApiValue());
            }
        }

        if (form.isRedriveEnabled() && deadLetterArn != null && !deadLetterArn.isBlank()) {
            attributes.put(QueueAttributeName.REDRIVE_POLICY, buildRedrivePolicy(deadLetterArn, form.getMaxReceiveCount()));
        }

        if (form.isRedriveAllowEnabled()) {
            attributes.put(QueueAttributeName.REDRIVE_ALLOW_POLICY, buildRedriveAllowPolicy(form));
        }

        applySseAttributes(attributes, form);

        if (form.getPolicy() != null && !form.getPolicy().isBlank()) {
            attributes.put(QueueAttributeName.POLICY, form.getPolicy().trim());
        }

        return attributes;
    }

    public Map<String, String> parseTags(QueueForm form) {
        if (form.getTagsText() == null || form.getTagsText().isBlank()) {
            return Map.of();
        }
        Map<String, String> tags = new LinkedHashMap<>();
        for (String line : form.getTagsText().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                throw new IllegalArgumentException("タグは key=value 形式で1行1つ指定してください: " + trimmed);
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            tags.put(key, value);
        }
        return tags;
    }

    public String resolveDeadLetterArn(QueueForm form, QueueService queueService) {
        if (!form.isRedriveEnabled()) {
            return null;
        }
        if (form.getDeadLetterTargetArn() != null && !form.getDeadLetterTargetArn().isBlank()) {
            return form.getDeadLetterTargetArn().trim();
        }
        if (form.getDeadLetterQueueName() != null && !form.getDeadLetterQueueName().isBlank()) {
            String name = form.getDeadLetterQueueName().trim();
            try {
                String url = queueService.getQueueUrl(name);
                return arnFromQueueUrl(url);
            } catch (Exception e) {
                return buildLocalArn(name);
            }
        }
        return null;
    }

    public String buildLocalArn(String queueName) {
        return "arn:aws:sqs:%s:000000000000:%s".formatted(awsSqsProperties.getRegion(), queueName);
    }

    private String arnFromQueueUrl(String queueUrl) {
        String name = queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
        return buildLocalArn(name);
    }

    private void applySseAttributes(Map<QueueAttributeName, String> attributes, QueueForm form) {
        SseModeOption mode = form.getSseMode() != null ? form.getSseMode() : SseModeOption.NONE;
        switch (mode) {
            case SQS_MANAGED -> attributes.put(QueueAttributeName.SQS_MANAGED_SSE_ENABLED, "true");
            case KMS -> {
                if (form.getKmsMasterKeyId() != null && !form.getKmsMasterKeyId().isBlank()) {
                    attributes.put(QueueAttributeName.KMS_MASTER_KEY_ID, form.getKmsMasterKeyId().trim());
                }
                if (form.getKmsDataKeyReusePeriodSeconds() != null) {
                    attributes.put(QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS,
                            String.valueOf(form.getKmsDataKeyReusePeriodSeconds()));
                }
            }
            default -> { /* no SSE attributes */ }
        }
    }

    private String buildRedrivePolicy(String deadLetterArn, Integer maxReceiveCount) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("deadLetterTargetArn", deadLetterArn);
        policy.put("maxReceiveCount", maxReceiveCount != null ? maxReceiveCount : 10);
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RedrivePolicy JSON の生成に失敗しました", e);
        }
    }

    private String buildRedriveAllowPolicy(QueueForm form) {
        Map<String, Object> policy = new LinkedHashMap<>();
        RedrivePermissionOption permission = form.getRedrivePermission() != null
                ? form.getRedrivePermission()
                : RedrivePermissionOption.ALLOW_ALL;
        policy.put("redrivePermission", permission.getApiValue());
        if (permission == RedrivePermissionOption.BY_QUEUE
                && form.getSourceQueueArns() != null
                && !form.getSourceQueueArns().isBlank()) {
            var arns = java.util.Arrays.stream(form.getSourceQueueArns().split("[,\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            policy.put("sourceQueueArns", arns);
        }
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RedriveAllowPolicy JSON の生成に失敗しました", e);
        }
    }
}
