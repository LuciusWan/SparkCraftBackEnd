package com.lucius.sparkcraftbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemoryEntity {
    private Long id;
    private String conversationId;
    private String messageType;
    private String content;
    private String metadata;
    private LocalDateTime createdAt;
}