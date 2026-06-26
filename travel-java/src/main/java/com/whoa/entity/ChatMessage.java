package com.whoa.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long userId;
    private String content;
    private String sessionId;
    private String role;
    private LocalDateTime createdAt;
}
