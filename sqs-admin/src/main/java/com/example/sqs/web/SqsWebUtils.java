package com.example.sqs.web;

import org.springframework.stereotype.Component;

@Component("sqsWeb")
public class SqsWebUtils {

    public String encodeQueueUrl(String queueUrl) {
        return QueueController.encodeUrl(queueUrl);
    }
}
