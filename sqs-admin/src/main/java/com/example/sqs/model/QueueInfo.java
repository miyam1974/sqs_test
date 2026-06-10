package com.example.sqs.model;

import java.util.Map;

public record QueueInfo(
        String name,
        String url,
        boolean fifo,
        Map<String, String> attributes
) {
}
