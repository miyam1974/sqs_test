package com.example.sqs.model;

public enum QueueType {
    STANDARD("標準"),
    FIFO("FIFO");

    private final String label;

    QueueType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFifo() {
        return this == FIFO;
    }
}
