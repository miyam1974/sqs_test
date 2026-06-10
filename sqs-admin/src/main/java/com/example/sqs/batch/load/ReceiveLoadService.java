package com.example.sqs.batch.load;

import com.example.sqs.service.MessageStorageService;
import com.example.sqs.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Profile("receive-load")
public class ReceiveLoadService {

    private static final Logger log = LoggerFactory.getLogger(ReceiveLoadService.class);

    private final SqsClient sqsClient;
    private final QueueService queueService;
    private final MessageStorageService messageStorageService;

    public ReceiveLoadService(
            SqsClient sqsClient,
            QueueService queueService,
            MessageStorageService messageStorageService) {
        this.sqsClient = sqsClient;
        this.queueService = queueService;
        this.messageStorageService = messageStorageService;
    }

    public LoadTestReport run(LoadTestCliArgs args) {
        String queueUrl = queueService.getQueueUrl(args.queueName());
        String queueName = args.queueName();

        LoadTestReport report = new LoadTestReport(
                "Receive",
                queueName,
                args.threads(),
                null,
                args.batchSize(),
                args.useBatchApi(),
                null);

        try (ExecutorService executor = Executors.newFixedThreadPool(args.threads())) {
            List<Future<ThreadResult>> futures = new ArrayList<>();
            for (int threadIndex = 0; threadIndex < args.threads(); threadIndex++) {
                int threadId = threadIndex;
                futures.add(executor.submit(() -> receiveFromThread(
                        queueUrl,
                        queueName,
                        threadId,
                        args.batchSize(),
                        args.useBatchApi())));
            }
            for (Future<ThreadResult> future : futures) {
                ThreadResult result = future.get();
                report.addThreadStats(result.threadIndex(), result.processed(), result.errors());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Receive load test failed: " + e.getMessage(), e);
        }
        return report;
    }

    private ThreadResult receiveFromThread(
            String queueUrl,
            String queueName,
            int threadIndex,
            int batchSize,
            boolean useBatchApi) {
        int processed = 0;
        int errors = 0;

        while (true) {
            int currentBatchSize = useBatchApi ? batchSize : 1;
            List<Message> messages;
            try {
                messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(currentBatchSize)
                        .waitTimeSeconds(0)
                        .messageAttributeNames("All")
                        .messageSystemAttributeNames(
                                MessageSystemAttributeName.MESSAGE_GROUP_ID,
                                MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID)
                        .build()).messages();
            } catch (Exception e) {
                log.error("thread-{} receive failed: {}", threadIndex, e.getMessage(), e);
                errors++;
                break;
            }

            if (messages == null || messages.isEmpty()) {
                log.info("thread-{} found no messages; stopping this thread", threadIndex);
                break;
            }

            if (!useBatchApi) {
                Message message = messages.getFirst();
                try {
                    String messageGroupId = message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID);
                    messageStorageService.save(queueName, queueUrl, message, messageGroupId);
                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build());
                    processed++;
                } catch (Exception e) {
                    errors++;
                    log.error("thread-{} failed to process message {}: {}",
                            threadIndex, message.messageId(), e.getMessage(), e);
                }
                continue;
            }

            List<DeleteMessageBatchRequestEntry> deleteEntries = new ArrayList<>();
            for (Message message : messages) {
                try {
                    String messageGroupId = message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID);
                    messageStorageService.save(queueName, queueUrl, message, messageGroupId);
                    deleteEntries.add(DeleteMessageBatchRequestEntry.builder()
                            .id(message.messageId())
                            .receiptHandle(message.receiptHandle())
                            .build());
                } catch (Exception e) {
                    errors++;
                    log.error("thread-{} failed to save message {}: {}",
                            threadIndex, message.messageId(), e.getMessage(), e);
                }
            }

            if (!deleteEntries.isEmpty()) {
                try {
                    sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
                            .queueUrl(queueUrl)
                            .entries(deleteEntries)
                            .build());
                    processed += deleteEntries.size();
                } catch (Exception e) {
                    errors += deleteEntries.size();
                    log.error("thread-{} delete batch failed: {}", threadIndex, e.getMessage(), e);
                    break;
                }
            }
        }

        log.info("thread-{} finished receiving: processed={} errors={}", threadIndex, processed, errors);
        return new ThreadResult(threadIndex, processed, errors);
    }

    private record ThreadResult(int threadIndex, int processed, int errors) {
    }
}
