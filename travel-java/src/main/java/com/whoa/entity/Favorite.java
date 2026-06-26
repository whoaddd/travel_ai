package com.whoa.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Favorite {
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String title;
    private LocalDateTime CreatedAt;
}
