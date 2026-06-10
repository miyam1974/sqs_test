package com.example.sqs.batch.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoadTestReport {

    private static final Logger log = LoggerFactory.getLogger(LoadTestReport.class);

    private final String kind;
    private final String queueName;
    private final int threads;
    private final Integer requestedTotal;
    private final int batchSize;
    private final boolean useBatchApi;
    private final Integer messageLengthBytes;
    private final long startedAtNanos;
    private final List<ThreadStats> threadStats = new ArrayList<>();

    private int processedTotal;
    private int errorTotal;

    public LoadTestReport(
            String kind,
            String queueName,
            int threads,
            Integer requestedTotal,
            int batchSize,
            boolean useBatchApi,
            Integer messageLengthBytes) {
        this.kind = kind;
        this.queueName = queueName;
        this.threads = threads;
        this.requestedTotal = requestedTotal;
        this.batchSize = batchSize;
        this.useBatchApi = useBatchApi;
        this.messageLengthBytes = messageLengthBytes;
        this.startedAtNanos = System.nanoTime();
    }

    public void addThreadStats(int threadIndex, int processed, int errors) {
        threadStats.add(new ThreadStats(threadIndex, processed, errors));
        processedTotal += processed;
        errorTotal += errors;
    }

    public boolean hasErrors() {
        return errorTotal > 0;
    }

    public void logSummary() {
        double elapsedSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;
        double throughput = elapsedSeconds > 0 ? processedTotal / elapsedSeconds : 0.0;

        log.info("========== {} Load Test Report ==========", kind);
        log.info("Queue:             {}", queueName);
        log.info("Threads:           {}", threads);
        if (requestedTotal != null) {
            log.info("Requested total:   {}", requestedTotal);
        } else {
            log.info("Stop condition:    until each thread receives empty");
        }
        log.info("Processed total:   {}", processedTotal);
        if (useBatchApi) {
            log.info("Batch size:        {} (batch API)", batchSize);
        } else {
            log.info("Batch size:        omitted (single API)");
        }
        if (messageLengthBytes != null) {
            log.info("Message length:    {} bytes", messageLengthBytes);
        }
        log.info("Duration:          {} s", format(elapsedSeconds));
        log.info("Throughput:        {} msg/s", format(throughput));
        log.info("Errors:            {}", errorTotal);
        for (ThreadStats stats : threadStats) {
            log.info("  thread-{}: processed={} errors={}", stats.threadIndex(), stats.processed(), stats.errors());
        }
        log.info("==========================================");
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private record ThreadStats(int threadIndex, int processed, int errors) {
    }
}
