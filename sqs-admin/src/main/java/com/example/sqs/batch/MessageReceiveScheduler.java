package com.example.sqs.batch;

import com.example.sqs.service.MessageReceiveBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("batch")
public class MessageReceiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageReceiveScheduler.class);

    private final MessageReceiveBatchService batchService;

    public MessageReceiveScheduler(MessageReceiveBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelayString = "${batch.poll-interval-ms:5000}")
    public void poll() {
        try {
            int count = batchService.processOnce();
            if (count > 0) {
                log.info("Batch processed {} message(s) from all queues", count);
            }
        } catch (Exception e) {
            log.error("Batch poll failed: {}", e.getMessage(), e);
        }
    }
}
