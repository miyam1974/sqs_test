package com.example.sqs.batch.load;

import com.example.sqs.model.QueueInfo;
import com.example.sqs.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Profile("send-load")
public class SendLoadService {

    private static final Logger log = LoggerFactory.getLogger(SendLoadService.class);

    private final SqsClient sqsClient;
    private final QueueService queueService;

    public SendLoadService(SqsClient sqsClient, QueueService queueService) {
        this.sqsClient = sqsClient;
        this.queueService = queueService;
    }

    public LoadTestReport run(LoadTestCliArgs args) {
        String queueUrl = queueService.getQueueUrl(args.queueName());
        QueueInfo queueInfo = queueService.getQueue(queueUrl);
        boolean fifo = queueInfo.fifo();
        boolean contentBasedDeduplication = isContentBasedDeduplicationEnabled(queueInfo);

        LoadTestReport report = new LoadTestReport(
                "Send",
                args.queueName(),
                args.threads(),
                args.totalCount(),
                args.batchSize(),
                args.useBatchApi(),
                args.messageLengthBytes());

        int[] perThreadCounts = CountSplitter.split(args.totalCount(), args.threads());
        try (ExecutorService executor = Executors.newFixedThreadPool(args.threads())) {
            List<Future<ThreadResult>> futures = new ArrayList<>();
            for (int threadIndex = 0; threadIndex < args.threads(); threadIndex++) {
                int count = perThreadCounts[threadIndex];
                if (count == 0) {
                    report.addThreadStats(threadIndex, 0, 0);
                    continue;
                }
                int threadId = threadIndex;
                futures.add(executor.submit(() -> sendFromThread(
                        queueUrl,
                        fifo,
                        contentBasedDeduplication,
                        threadId,
                        count,
                        args.batchSize(),
                        args.useBatchApi(),
                        args.messageLengthBytes())));
            }
            for (int i = 0; i < futures.size(); i++) {
                ThreadResult result = futures.get(i).get();
                report.addThreadStats(result.threadIndex(), result.processed(), result.errors());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Send load test failed: " + e.getMessage(), e);
        }
        return report;
    }

    private ThreadResult sendFromThread(
            String queueUrl,
            boolean fifo,
            boolean contentBasedDeduplication,
            int threadIndex,
            int targetCount,
            int batchSize,
            boolean useBatchApi,
            int messageLengthBytes) {
        String messageGroupId = "thread-" + threadIndex;
        int processed = 0;
        int errors = 0;
        int remaining = targetCount;

        while (remaining > 0) {
            int currentBatchSize = Math.min(batchSize, remaining);
            try {
                if (useBatchApi) {
                    int succeeded = sendBatch(
                            queueUrl,
                            fifo,
                            contentBasedDeduplication,
                            threadIndex,
                            processed,
                            currentBatchSize,
                            messageLengthBytes);
                    int failed = currentBatchSize - succeeded;
                    processed += succeeded;
                    errors += failed;
                    remaining -= succeeded;
                    if (succeeded == 0) {
                        break;
                    }
                } else {
                    sendSingle(queueUrl, fifo, contentBasedDeduplication, messageGroupId, messageLengthBytes);
                    processed++;
                    remaining--;
                }
            } catch (Exception e) {
                errors += currentBatchSize;
                log.error("thread-{} send failed: {}", threadIndex, e.getMessage(), e);
                break;
            }
        }

        log.info("thread-{} finished sending: processed={} errors={}", threadIndex, processed, errors);
        return new ThreadResult(threadIndex, processed, errors);
    }

    private void sendSingle(
            String queueUrl,
            boolean fifo,
            boolean contentBasedDeduplication,
            String messageGroupId,
            int messageLengthBytes) {
        SendMessageRequest.Builder builder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(RandomMessageGenerator.randomBody(messageLengthBytes));
        if (fifo) {
            builder.messageGroupId(messageGroupId);
            if (!contentBasedDeduplication) {
                builder.messageDeduplicationId(UUID.randomUUID().toString());
            }
        }
        sqsClient.sendMessage(builder.build());
    }

    private int sendBatch(
            String queueUrl,
            boolean fifo,
            boolean contentBasedDeduplication,
            int threadIndex,
            int processed,
            int currentBatchSize,
            int messageLengthBytes) {
        String messageGroupId = "thread-" + threadIndex;
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>(currentBatchSize);
        for (int i = 0; i < currentBatchSize; i++) {
            SendMessageBatchRequestEntry.Builder entryBuilder = SendMessageBatchRequestEntry.builder()
                    .id("t" + threadIndex + "-m" + (processed + i))
                    .messageBody(RandomMessageGenerator.randomBody(messageLengthBytes));
            if (fifo) {
                entryBuilder.messageGroupId(messageGroupId);
                if (!contentBasedDeduplication) {
                    entryBuilder.messageDeduplicationId(UUID.randomUUID().toString());
                }
            }
            entries.add(entryBuilder.build());
        }

        SendMessageBatchResponse response = sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build());
        int failed = response.failed() != null ? response.failed().size() : 0;
        if (failed > 0) {
            log.warn("thread-{} send batch had {} failure(s): {}",
                    threadIndex,
                    failed,
                    formatFailures(response.failed()));
        }
        return currentBatchSize - failed;
    }

    private static boolean isContentBasedDeduplicationEnabled(QueueInfo info) {
        String value = info.attributes().get(QueueAttributeName.CONTENT_BASED_DEDUPLICATION.toString());
        return "true".equalsIgnoreCase(value);
    }

    private static String formatFailures(List<BatchResultErrorEntry> failed) {
        if (failed == null || failed.isEmpty()) {
            return "none";
        }
        return failed.stream()
                .map(entry -> entry.id() + ":" + entry.code() + ":" + entry.message())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
    }

    private record ThreadResult(int threadIndex, int processed, int errors) {
    }
}
