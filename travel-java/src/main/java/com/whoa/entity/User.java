package com.whoa.entity;


import lombok.Data;

import java.time.LocalDateTime;
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private LocalDateTime CreatedAt;
    private LocalDateTime UpdatedAt;
}
