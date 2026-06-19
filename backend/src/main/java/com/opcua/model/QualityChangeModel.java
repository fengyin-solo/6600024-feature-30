package com.opcua.model;

import java.time.Instant;

/**
 * 质量变化记录模型
 */
public class QualityChangeModel {

    private String from;
    private String to;
    private Instant timestamp;

    public QualityChangeModel() {
    }

    public QualityChangeModel(String from, String to, Instant timestamp) {
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
