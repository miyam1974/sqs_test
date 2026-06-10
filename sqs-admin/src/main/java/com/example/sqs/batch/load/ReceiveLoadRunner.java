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
@Profile("receive-load")
public class ReceiveLoadRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReceiveLoadRunner.class);

    private final ReceiveLoadService receiveLoadService;
    private final ConfigurableApplicationContext applicationContext;

    public ReceiveLoadRunner(ReceiveLoadService receiveLoadService, ConfigurableApplicationContext applicationContext) {
        this.receiveLoadService = receiveLoadService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            LoadTestCliArgs cliArgs = LoadTestCliArgs.parseReceive(args);
            log.info("Starting receive load test: queue={} threads={} stop=each-thread-until-empty batchSize={} useBatchApi={}",
                    cliArgs.queueName(),
                    cliArgs.threads(),
                    cliArgs.useBatchApi() ? cliArgs.batchSize() : "omitted",
                    cliArgs.useBatchApi());
            LoadTestReport report = receiveLoadService.run(cliArgs);
            report.logSummary();
            exit(report.hasErrors() ? 1 : 0);
        } catch (Exception e) {
            log.error("Receive load test aborted: {}", e.getMessage(), e);
            exit(1);
        }
    }

    private void exit(int code) {
        SpringApplication.exit(applicationContext, () -> code);
    }
}
