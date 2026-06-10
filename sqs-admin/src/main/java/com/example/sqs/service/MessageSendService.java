package com.example.sqs.service;

import com.example.sqs.model.QueueInfo;
import com.example.sqs.web.dto.SendMessageForm;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.UUID;

@Service
public class MessageSendService {

    private final SqsClient sqsClient;
    private final QueueService queueService;

    public MessageSendService(SqsClient sqsClient, QueueService queueService) {
        this.sqsClient = sqsClient;
        this.queueService = queueService;
    }

    public SendMessageResponse send(SendMessageForm form) {
        var info = queueService.getQueue(form.getQueueUrl());
        var builder = SendMessageRequest.builder()
                .queueUrl(form.getQueueUrl())
                .messageBody(form.getBody());

        if (form.getDelaySeconds() != null && form.getDelaySeconds() > 0 && !info.fifo()) {
            builder.delaySeconds(form.getDelaySeconds());
        }

        if (info.fifo()) {
            String groupId = form.getMessageGroupId();
            if (groupId == null || groupId.isBlank()) {
                groupId = "default-group";
            }
            builder.messageGroupId(groupId);
            String dedupId = form.getMessageDeduplicationId();
            if (dedupId != null && !dedupId.isBlank()) {
                builder.messageDeduplicationId(dedupId.trim());
            } else if (!isContentBasedDeduplicationEnabled(info)) {
                // ContentBasedDeduplication 無効時は MessageDeduplicationId が必須
                builder.messageDeduplicationId(UUID.randomUUID().toString());
            }
        }

        return sqsClient.sendMessage(builder.build());
    }

    private static boolean isContentBasedDeduplicationEnabled(QueueInfo info) {
        String value = info.attributes().get(QueueAttributeName.CONTENT_BASED_DEDUPLICATION.toString());
        return "true".equalsIgnoreCase(value);
    }
}
