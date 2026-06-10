package com.example.sqs.batch.load;

import org.springframework.boot.ApplicationArguments;

public record LoadTestCliArgs(
        String queueName,
        int threads,
        Integer totalCount,
        int batchSize,
        boolean useBatchApi,
        Integer messageLengthBytes
) {

    public static LoadTestCliArgs parseSend(ApplicationArguments args) {
        String queueName = require(args, "queue-name");
        int threads = parseInt(require(args, "threads"), "threads", 1, Integer.MAX_VALUE);
        int totalCount = parseInt(require(args, "total-count"), "total-count", 1, Integer.MAX_VALUE);
        BatchSizeOption batchSizeOption = parseBatchSize(args);
        int messageLength = parseInt(require(args, "message-length"), "message-length", 1, 256 * 1024);
        return new LoadTestCliArgs(
                queueName,
                threads,
                totalCount,
                batchSizeOption.value(),
                batchSizeOption.useBatchApi(),
                messageLength);
    }

    public static LoadTestCliArgs parseReceive(ApplicationArguments args) {
        if (args.containsOption("total-count")) {
            throw new IllegalArgumentException("Receive load test does not support --total-count");
        }
        String queueName = require(args, "queue-name");
        int threads = parseInt(require(args, "threads"), "threads", 1, Integer.MAX_VALUE);
        BatchSizeOption batchSizeOption = parseBatchSize(args);
        return new LoadTestCliArgs(
                queueName,
                threads,
                null,
                batchSizeOption.value(),
                batchSizeOption.useBatchApi(),
                null);
    }

    private static BatchSizeOption parseBatchSize(ApplicationArguments args) {
        if (!args.containsOption("batch-size")) {
            return new BatchSizeOption(1, false);
        }
        var values = args.getOptionValues("batch-size");
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            return new BatchSizeOption(1, false);
        }
        return new BatchSizeOption(parseInt(values.getFirst().trim(), "batch-size", 1, 10), true);
    }

    private record BatchSizeOption(int value, boolean useBatchApi) {
    }

    private static String require(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            throw new IllegalArgumentException("Missing required argument: --" + name);
        }
        var values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("Missing required argument: --" + name);
        }
        return values.getFirst().trim();
    }

    private static int parseInt(String value, String name, int min, int max) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " must be an integer: " + value);
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException("--" + name + " must be between " + min + " and " + max);
        }
        return parsed;
    }
}
