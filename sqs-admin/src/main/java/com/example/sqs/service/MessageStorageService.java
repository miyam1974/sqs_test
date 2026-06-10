package com.example.sqs.service;

import com.example.sqs.config.MessageStorageProperties;
import com.example.sqs.model.SavedMessageRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());

    private final Path storageRoot;
    private final ObjectMapper objectMapper;

    public MessageStorageService(MessageStorageProperties properties) {
        this.storageRoot = Path.of(properties.getStorageDir());
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Path save(String queueName, String queueUrl, Message message) throws IOException {
        return save(queueName, queueUrl, message, null);
    }

    public Path save(String queueName, String queueUrl, Message message, String messageGroupId) throws IOException {
        Instant receivedAt = Instant.now();
        Map<String, String> attributes = new HashMap<>();
        if (message.messageAttributes() != null) {
            message.messageAttributes().forEach((k, v) ->
                    attributes.put(k, v.stringValue() != null ? v.stringValue() : ""));
        }

        SavedMessageRecord record = new SavedMessageRecord(
                message.messageId(),
                message.receiptHandle(),
                message.body(),
                attributes,
                queueName,
                queueUrl,
                receivedAt,
                null
        );

        String datePart = DATE_DIR.format(receivedAt);
        Path dir = storageRoot.resolve(sanitize(queueName));
        if (messageGroupId != null && !messageGroupId.isBlank()) {
            dir = dir.resolve(sanitize(messageGroupId));
        }
        dir = dir.resolve(datePart);
        Files.createDirectories(dir);

        String safeId = sanitize(message.messageId());
        Path file = dir.resolve(safeId + ".json");

        SavedMessageRecord withPath = new SavedMessageRecord(
                record.messageId(),
                record.receiptHandle(),
                record.body(),
                record.attributes(),
                record.queueName(),
                record.queueUrl(),
                record.receivedAt(),
                file.toAbsolutePath().toString()
        );

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), withPath);
        return file;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
