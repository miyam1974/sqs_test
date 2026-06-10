package com.example.sqs.batch.load;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

final class RandomMessageGenerator {

    private RandomMessageGenerator() {
    }

    static String randomBody(int lengthBytes) {
        byte[] bytes = new byte[lengthBytes];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < lengthBytes; i++) {
            // Printable ASCII (SQS / ElasticMQ XML-safe)
            bytes[i] = (byte) (32 + random.nextInt(95));
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
