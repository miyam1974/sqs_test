package com.example.sqs.service;

import com.example.sqs.model.QueueInfo;
import com.example.sqs.model.QueueType;
import com.example.sqs.web.dto.QueueEditForm;
import com.example.sqs.web.dto.QueueForm;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueueService {

    private static final List<QueueAttributeName> DISPLAY_ATTRIBUTES = List.of(
            QueueAttributeName.VISIBILITY_TIMEOUT,
            QueueAttributeName.MESSAGE_RETENTION_PERIOD,
            QueueAttributeName.DELAY_SECONDS,
            QueueAttributeName.MAXIMUM_MESSAGE_SIZE,
            QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS,
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
            QueueAttributeName.FIFO_QUEUE,
            QueueAttributeName.CONTENT_BASED_DEDUPLICATION,
            QueueAttributeName.REDRIVE_POLICY,
            QueueAttributeName.KMS_MASTER_KEY_ID,
            QueueAttributeName.SQS_MANAGED_SSE_ENABLED
    );

    private final SqsClient sqsClient;
    private final QueueAttributeBuilder attributeBuilder;

    public QueueService(SqsClient sqsClient, QueueAttributeBuilder attributeBuilder) {
        this.sqsClient = sqsClient;
        this.attributeBuilder = attributeBuilder;
    }

    public List<QueueInfo> listQueues() {
        var urls = sqsClient.listQueues(ListQueuesRequest.builder().build()).queueUrls();
        return urls.stream().map(this::loadQueueInfo).collect(Collectors.toList());
    }

    /** バッチ用: 属性取得なしでキュー一覧（ListQueues のみ） */
    public List<QueueInfo> listQueuesBrief() {
        var urls = sqsClient.listQueues(ListQueuesRequest.builder().build()).queueUrls();
        return urls.stream()
                .map(url -> {
                    String name = extractQueueName(url);
                    return new QueueInfo(name, url, name.endsWith(".fifo"), Map.of());
                })
                .collect(Collectors.toList());
    }

    public QueueInfo getQueue(String queueUrl) {
        return loadQueueInfo(queueUrl);
    }

    public String createQueue(QueueForm form) {
        String queueName = form.resolvedQueueName();
        String deadLetterArn = attributeBuilder.resolveDeadLetterArn(form, this);
        Map<QueueAttributeName, String> attributes = attributeBuilder.buildCreateAttributes(form, deadLetterArn);
        Map<String, String> tags = attributeBuilder.parseTags(form);

        var requestBuilder = CreateQueueRequest.builder()
                .queueName(queueName)
                .attributes(attributes);
        if (!tags.isEmpty()) {
            requestBuilder.tags(tags);
        }

        return sqsClient.createQueue(requestBuilder.build()).queueUrl();
    }

    public void updateQueue(QueueEditForm form) {
        Map<QueueAttributeName, String> attributes = new java.util.HashMap<>();
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(form.getVisibilityTimeout()));
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, String.valueOf(form.getMessageRetentionPeriod()));
        if (!form.isFifo()) {
            attributes.put(QueueAttributeName.DELAY_SECONDS, String.valueOf(form.getDelaySeconds()));
        }
        if (form.isFifo() && form.getContentBasedDeduplication() != null) {
            attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION,
                    form.getContentBasedDeduplication() ? "true" : "false");
        }
        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
                .queueUrl(form.getQueueUrl())
                .attributes(attributes)
                .build());
    }

    public void deleteQueue(String queueUrl) {
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    public String getQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }

    public QueueEditForm toEditForm(String queueUrl) {
        QueueInfo info = getQueue(queueUrl);
        QueueEditForm form = new QueueEditForm();
        form.setQueueUrl(info.url());
        form.setName(info.name());
        form.setQueueType(info.fifo() ? QueueType.FIFO : QueueType.STANDARD);
        form.setVisibilityTimeout(parseIntAttr(info.attributes(), QueueAttributeName.VISIBILITY_TIMEOUT.toString(), 30));
        form.setMessageRetentionPeriod(parseIntAttr(info.attributes(), QueueAttributeName.MESSAGE_RETENTION_PERIOD.toString(), 345600));
        form.setDelaySeconds(parseIntAttr(info.attributes(), QueueAttributeName.DELAY_SECONDS.toString(), 0));
        String dedup = info.attributes().get(QueueAttributeName.CONTENT_BASED_DEDUPLICATION.toString());
        if (dedup != null) {
            form.setContentBasedDeduplication("true".equals(dedup));
        }
        return form;
    }

    private QueueInfo loadQueueInfo(String queueUrl) {
        var attrs = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(DISPLAY_ATTRIBUTES)
                .build()).attributes();

        Map<String, String> attrMap = attrs.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

        String name = extractQueueName(queueUrl);
        boolean fifo = "true".equals(attrMap.getOrDefault(QueueAttributeName.FIFO_QUEUE.toString(), "false"))
                || name.endsWith(".fifo");

        return new QueueInfo(name, queueUrl, fifo, attrMap);
    }

    private static String extractQueueName(String queueUrl) {
        int lastSlash = queueUrl.lastIndexOf('/');
        return lastSlash >= 0 ? queueUrl.substring(lastSlash + 1) : queueUrl;
    }

    private static int parseIntAttr(Map<String, String> attrs, String key, int defaultValue) {
        String value = attrs.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
