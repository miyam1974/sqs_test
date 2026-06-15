package com.example.sqs.batch.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("send-load")
public class SendLoadRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SendLoadRunner.class);

    private final SendLoadService sendLoadService;
    private final ConfigurableApplicationContext applicationContext;

    public SendLoadRunner(SendLoadService sendLoadService, ConfigurableApplicationContext applicationContext) {
        this.sendLoadService = sendLoadService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            LoadTestCliArgs cliArgs = LoadTestCliArgs.parseSend(args);
            log.info("Starting send load test: queue={} threads={} total={} batchSize={} useBatchApi={} messageLength={} messageGroup={}",
                    cliArgs.queueName(),
                    cliArgs.threads(),
                    cliArgs.totalCount(),
                    cliArgs.useBatchApi() ? cliArgs.batchSize() : "omitted",
                    cliArgs.useBatchApi(),
                    cliArgs.messageLengthBytes(),
                    formatMessageGroupMode(cliArgs.sharedMessageGroupId()));
            LoadTestReport report = sendLoadService.run(cliArgs);
            report.logSummary();
            exit(report.hasErrors() ? 1 : 0);
        } catch (Exception e) {
            log.error("Send load test aborted: {}", e.getMessage(), e);
            exit(1);
        }
    }

    private void exit(int code) {
        SpringApplication.exit(applicationContext, () -> code);
    }

    private static String formatMessageGroupMode(String sharedMessageGroupId) {
        if (sharedMessageGroupId != null) {
            return sharedMessageGroupId + " (shared across threads)";
        }
        return "thread-{n} per thread";
    }
}
