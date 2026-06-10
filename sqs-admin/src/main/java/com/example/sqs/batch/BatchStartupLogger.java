package com.example.sqs.batch;

import com.example.sqs.config.BatchProperties;
import com.example.sqs.config.MessageStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("batch")
public class BatchStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(BatchStartupLogger.class);

    private final BatchProperties batchProperties;
    private final MessageStorageProperties storageProperties;

    public BatchStartupLogger(BatchProperties batchProperties, MessageStorageProperties storageProperties) {
        this.batchProperties = batchProperties;
        this.storageProperties = storageProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("SQS receive batch started. target=all queues, storageDir={}, intervalMs={}",
                storageProperties.getStorageDir(),
                batchProperties.getPollIntervalMs());
    }
}
