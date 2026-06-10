package com.example.sqs.model;

import java.time.Instant;
import java.util.Map;

public record SavedMessageRecord(
        String messageId,
        String receiptHandle,
        String body,
        Map<String, String> attributes,
        String queueName,
        String queueUrl,
        Instant receivedAt,
        String savedPath
) {
}
