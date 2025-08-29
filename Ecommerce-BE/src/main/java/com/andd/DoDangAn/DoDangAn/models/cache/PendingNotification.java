package com.andd.DoDangAn.DoDangAn.models.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class PendingNotification {
    @Id
    private String id = UUID.randomUUID().toString();
    private String channel;
    private String payload;
    private LocalDateTime createdAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}