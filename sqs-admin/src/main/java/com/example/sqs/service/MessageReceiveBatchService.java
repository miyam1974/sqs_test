package com.example.sqs.service;

import com.example.sqs.config.BatchProperties;
import com.example.sqs.model.QueueInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageReceiveBatchService {

    private static final Logger log = LoggerFactory.getLogger(MessageReceiveBatchService.class);

    private final SqsClient sqsClient;
    private final QueueService queueService;
    private final MessageStorageService messageStorageService;
    private final BatchProperties batchProperties;

    public MessageReceiveBatchService(
            SqsClient sqsClient,
            QueueService queueService,
            MessageStorageService messageStorageService,
            BatchProperties batchProperties) {
        this.sqsClient = sqsClient;
        this.queueService = queueService;
        this.messageStorageService = messageStorageService;
        this.batchProperties = batchProperties;
    }

    public int processOnce() {
        List<QueueInfo> queues = queueService.listQueuesBrief();
        if (queues.isEmpty()) {
            log.debug("No queues to poll");
            return 0;
        }
        int total = 0;
        for (var queue : queues) {
            try {
                total += processQueue(queue.url(), queue.name());
            } catch (Exception e) {
                log.error("Failed to poll queue {}: {}", queue.name(), e.getMessage(), e);
            }
        }
        return total;
    }

    public int processQueue(String queueUrl, String queueName) {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(batchProperties.getMaxMessages())
                .waitTimeSeconds(batchProperties.getWaitTimeSeconds())
                .messageAttributeNames("All")
                .build()).messages();

        if (messages.isEmpty()) {
            log.debug("No messages in queue {}", queueName);
            return 0;
        }

        int processed = 0;
        List<String> errors = new ArrayList<>();

        for (Message message : messages) {
            try {
                var path = messageStorageService.save(queueName, queueUrl, message);
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
                log.info("Saved and deleted message {} -> {}", message.messageId(), path);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process message {}: {}", message.messageId(), e.getMessage(), e);
                errors.add(message.messageId());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Failed message IDs (left in queue): {}", errors);
        }
        return processed;
    }
}
