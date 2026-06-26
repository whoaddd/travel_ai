# AI旅行助手 - 详细实施文档

> 本文档手把手教你每一步怎么写，包含每个文件的完整代码。按顺序从第1步做到第6步即可。

---

## 第1步：数据库建表

打开 MySQL 客户端（Navicat / 命令行都行），先建数据库，再建三张表：

```sql
CREATE DATABASE travel_ai DEFAULT CHARSET utf8mb4;

USE travel_ai;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(36) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user / assistant',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` VARCHAR(20) NOT NULL COMMENT '收藏类型：itinerary / message',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '收藏标题',
  `content` TEXT NOT NULL COMMENT '收藏内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

---

## 第2步：pom.xml 添加依赖

你的 pom.xml 中已经有 mybatis、jwt、mysql-connector 了，**还需要加一个 BCrypt 依赖**用于密码加密。

在 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

> 加完后右键 pom.xml → Maven → Reload Project

---

## 第3步：application.yaml 配置

你的 yaml 中已经有数据源、mybatis、jwt 配置了，**还需要加两样东西**：

### 3.1 在文件末尾加日志配置

```yaml
logging:
  level:
    com.whoa: debug
```

### 3.2 jwt.secret 改长一点

当前你写的是 `whoa-ai-secret`，太短了。JWT 密钥要求至少 256 位（32 字节），改成至少 32 字符的随机字符串，例如：

```yaml
jwt:
  secret: whoa-ai-travel-secret-key-2024-must-be-long-enough
  expiration: 86400000
```

---

## 第4步：启动类加 MapperScan

打开 `TravelJavaApplication.java`，在 `@SpringBootApplication` 下面加一行：

```java
package com.whoa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.whoa.mapper")   // ← 新增这一行
public class TravelJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelJavaApplication.class, args);
    }
}
```

---

## 第5步：创建 Entity 实体类

在 `src/main/java/com/whoa/entity/` 下创建三个文件。

### 5.1 User.java

```java
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

> 字段名用驼峰，mybatis 配了 `map-underscore-to-camel-case: true`，会自动对应数据库的 `created_at` 等下划线字段。

### 5.2 ChatMessage.java

```java
package com.whoa.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long userId;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
```

### 5.3 Favorite.java

```java
package com.whoa.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Favorite {
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
```

---

## 第6步：创建 Mapper 接口

在 `src/main/java/com/whoa/mapper/` 下创建三个文件。

### 6.1 UserMapper.java

```java
package com.whoa.mapper;

import com.whoa.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    void insert(User user);

    User findById(@Param("id") Long id);
}
```

### 6.2 ChatMessageMapper.java

```java
package com.whoa.mapper;

import com.whoa.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessage chatMessage);

    List<ChatMessage> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    List<Map<String, Object>> findSessionsByUserId(@Param("userId") Long userId);

    void deleteBySessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
```

> `findSessionsByUserId` 返回 `List<Map>` 是因为这个查询是 GROUP BY session_id，只返回 sessionId 和最后一条消息的摘要，不是完整的 ChatMessage 对象。

### 6.3 FavoriteMapper.java

```java
package com.whoa.mapper;

import com.whoa.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    void insert(Favorite favorite);

    List<Favorite> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    List<Favorite> findByUserId(@Param("userId") Long userId);

    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
```

---

## 第7步：创建 Mapper XML

在 `src/main/resources/mapper/` 下创建三个 XML 文件。

### 7.1 UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.whoa.mapper.UserMapper">

    <select id="findByUsername" resultType="com.whoa.entity.User">
        SELECT id, username, password, nickname, avatar, created_at, updated_at
        FROM user
        WHERE username = #{username}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, nickname, avatar)
        VALUES (#{username}, #{password}, #{nickname}, #{avatar})
    </insert>

    <select id="findById" resultType="com.whoa.entity.User">
        SELECT id, username, password, nickname, avatar, created_at, updated_at
        FROM user
        WHERE id = #{id}
    </select>

</mapper>
```

> `useGeneratedKeys="true" keyProperty="id"` — 插入后自动把生成的主键回填到 user.id，注册时需要用。

### 7.2 ChatMessageMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.whoa.mapper.ChatMessageMapper">

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO chat_message (user_id, session_id, role, content)
        VALUES (#{userId}, #{sessionId}, #{role}, #{content})
    </insert>

    <select id="findByUserIdAndSessionId" resultType="com.whoa.entity.ChatMessage">
        SELECT id, user_id, session_id, role, content, created_at
        FROM chat_message
        WHERE user_id = #{userId} AND session_id = #{sessionId}
        ORDER BY created_at ASC
    </select>

    <select id="findSessionsByUserId" resultType="java.util.Map">
        SELECT session_id AS sessionId,
               MAX(content) AS lastMessage,
               MAX(created_at) AS lastTime,
               COUNT(*) AS messageCount
        FROM chat_message
        WHERE user_id = #{userId} AND role = 'user'
        GROUP BY session_id
        ORDER BY lastTime DESC
    </select>

    <delete id="deleteBySessionId">
        DELETE FROM chat_message
        WHERE user_id = #{userId} AND session_id = #{sessionId}
    </delete>

</mapper>
```

### 7.3 FavoriteMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.whoa.mapper.FavoriteMapper">

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO favorite (user_id, type, title, content)
        VALUES (#{userId}, #{type}, #{title}, #{content})
    </insert>

    <select id="findByUserIdAndType" resultType="com.whoa.entity.Favorite">
        SELECT id, user_id, type, title, content, created_at
        FROM favorite
        WHERE user_id = #{userId} AND type = #{type}
        ORDER BY created_at DESC
    </select>

    <select id="findByUserId" resultType="com.whoa.entity.Favorite">
        SELECT id, user_id, type, title, content, created_at
        FROM favorite
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
    </select>

    <delete id="deleteByIdAndUserId">
        DELETE FROM favorite
        WHERE id = #{id} AND user_id = #{userId}
    </delete>

</mapper>
```

---

## 第8步：创建 JWT 工具类和配置类

### 8.1 JwtProperties.java — 读取 yaml 中的 jwt 配置

在 `src/main/java/com/whoa/config/` 下创建：

```java
package com.whoa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long expiration;
}
```

> `@ConfigurationProperties(prefix = "jwt")` 会自动读取 yaml 中 `jwt.secret` 和 `jwt.expiration`。

### 8.2 JwtUtils.java — 生成和解析 token

在 `src/main/java/com/whoa/utils/` 下创建：

```java
package com.whoa.utils;

import com.whoa.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // 生成 token
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(getKey())
                .compact();
    }

    // 解析 token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 从 token 中取 userId
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    // 从 token 中取 username
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
}
```

---

## 第9步：创建密码加密工具

在 `src/main/java/com/whoa/utils/` 下创建 `PasswordUtils.java`：

```java
package com.whoa.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // 加密密码
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // 校验密码
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
```

> 两个方法都是 static 的，不需要注入，直接 `PasswordUtils.hashPassword("123456")` 这样调用。

---

## 第10步：创建 JWT 拦截器

### 10.1 JwtInterceptor.java

在 `src/main/java/com/whoa/interceptor/` 下创建：

```java
package com.whoa.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoa.utils.JwtUtils;
import com.whoa.vo.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求（跨域）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未携带token: {}", request.getRequestURI());
            writeError(response, 401, "未登录");
            return false;
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 把 userId 和 username 存到 request 中，后续 Controller 可以直接取
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("token已过期: {}", e.getMessage());
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("token格式错误: {}", e.getMessage());
            writeError(response, 401, "无效的token");
            return false;
        } catch (Exception e) {
            log.error("token解析异常: {}", e.getMessage());
            writeError(response, 401, "认证失败");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
```

> **关键点**：拦截器解析成功后，把 `userId` 放进 `request.setAttribute`，Controller 中通过 `request.getAttribute("userId")` 获取，这样 userId 就是 token 中解析出来的，前端无法伪造。

### 10.2 WebMvcConfig.java — 注册拦截器

在 `src/main/java/com/whoa/config/` 下创建：

```java
package com.whoa.config;

import com.whoa.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/travel/hello"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

> `excludePathPatterns` 里的路径不需要登录就能访问，其他的 `/api/**` 都要经过拦截器校验。

---

## 第11步：创建 Service 层

### 11.1 UserService.java

在 `src/main/java/com/whoa/service/` 下创建：

```java
package com.whoa.service;

import com.whoa.entity.User;
import com.whoa.mapper.UserMapper;
import com.whoa.utils.JwtUtils;
import com.whoa.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    // 注册
    public String register(String username, String password, String nickname) {
        // 1. 检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            log.warn("注册失败，用户名已存在: {}", username);
            throw new RuntimeException("用户名已存在");
        }

        // 2. 加密密码
        String hashedPassword = PasswordUtils.hashPassword(password);

        // 3. 插入数据库
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setNickname(nickname != null ? nickname : username);
        userMapper.insert(user);

        log.info("用户注册成功: {}", username);

        // 4. 生成 token 返回
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    // 登录
    public String login(String username, String password) {
        // 1. 查用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("登录失败，用户不存在: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            log.warn("登录失败，密码错误: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("用户登录成功: {}", username);

        // 3. 生成 token 返回
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    // 获取用户信息
    public User getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null); // 返回前清空密码
        }
        return user;
    }
}
```

> **安全注意**：登录失败时不告诉用户"密码错误"还是"用户不存在"，统一返回"用户名或密码错误"，防止攻击者枚举用户名。

### 11.2 ChatMessageService.java

在 `src/main/java/com/whoa/service/` 下创建：

```java
package com.whoa.service;

import com.whoa.entity.ChatMessage;
import com.whoa.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    // 保存一条消息
    public void saveMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        chatMessageMapper.insert(msg);
        log.debug("保存消息: userId={}, sessionId={}, role={}", userId, sessionId, role);
    }

    // 获取用户的所有会话列表
    public List<Map<String, Object>> getSessions(Long userId) {
        return chatMessageMapper.findSessionsByUserId(userId);
    }

    // 获取某个会话的消息列表
    public List<ChatMessage> getMessages(Long userId, String sessionId) {
        return chatMessageMapper.findByUserIdAndSessionId(userId, sessionId);
    }

    // 删除某个会话
    public void deleteSession(Long userId, String sessionId) {
        chatMessageMapper.deleteBySessionId(userId, sessionId);
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);
    }
}
```

### 11.3 FavoriteService.java

在 `src/main/java/com/whoa/service/` 下创建：

```java
package com.whoa.service;

import com.whoa.entity.Favorite;
import com.whoa.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    // 添加收藏
    public void addFavorite(Long userId, String type, String title, String content) {
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setType(type);
        fav.setTitle(title);
        fav.setContent(content);
        favoriteMapper.insert(fav);
        log.info("添加收藏: userId={}, type={}", userId, type);
    }

    // 查收藏列表（可按类型筛选）
    public List<Favorite> listFavorites(Long userId, String type) {
        if (type != null && !type.isEmpty()) {
            return favoriteMapper.findByUserIdAndType(userId, type);
        }
        return favoriteMapper.findByUserId(userId);
    }

    // 取消收藏
    public void removeFavorite(Long userId, Long id) {
        favoriteMapper.deleteByIdAndUserId(id, userId);
        log.info("取消收藏: userId={}, favoriteId={}", userId, id);
    }
}
```

---

## 第12步：创建 DTO

### 12.1 LoginDTO.java

在 `src/main/java/com/whoa/dto/` 下创建：

```java
package com.whoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;
}
```

### 12.2 RegisterDTO.java

在 `src/main/java/com/whoa/dto/` 下创建：

```java
package com.whoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名2-20个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    private String nickname;
}
```

### 12.3 FavoriteDTO.java

在 `src/main/java/com/whoa/dto/` 下创建：

```java
package com.whoa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteDTO {
    @NotBlank(message = "收藏类型不能为空")
    private String type;    // itinerary 或 message

    private String title;

    @NotBlank(message = "收藏内容不能为空")
    private String content;
}
```

### 12.4 修改 ChatRequestDTO.java — 增加 sessionId

你现有的 `ChatRequestDTO.java`，增加 `sessionId` 字段：

```java
package com.whoa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequestDTO {
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;   // ← 新增

    @NotBlank(message = "消息不能为空")
    private String message;
}
```

---

## 第13步：创建 Controller 层

### 13.1 UserController.java

在 `src/main/java/com/whoa/controller/` 下创建：

```java
package com.whoa.controller;

import com.whoa.dto.LoginDTO;
import com.whoa.dto.RegisterDTO;
import com.whoa.entity.User;
import com.whoa.service.UserService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        String token = userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getNickname()
        );
        return Result.ok(token);
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );
        return Result.ok(token);
    }

    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return Result.ok(user);
    }
}
```

> `/api/user/login` 和 `/api/user/register` 不需要 token，已经在拦截器中排除了。
> `/api/user/info` 需要 token，userId 从 request.getAttribute 中取。

### 13.2 ChatController.java

在 `src/main/java/com/whoa/controller/` 下创建：

```java
package com.whoa.controller;

import com.whoa.dto.ChatRequestDTO;
import com.whoa.entity.ChatMessage;
import com.whoa.service.ChatMessageService;
import com.whoa.service.TravelService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final TravelService travelService;
    private final ChatMessageService chatMessageService;

    // 发起对话（流式）
    @PostMapping(value = "/send", produces = "text/event-stream")
    public SseEmitter chat(@Valid @RequestBody ChatRequestDTO chatRequestDTO,
                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String sessionId = chatRequestDTO.getSessionId();
        String message = chatRequestDTO.getMessage();

        // 保存用户消息
        chatMessageService.saveMessage(userId, sessionId, "user", message);

        // 调用已有的 chat 方法，但传入 userId 和 sessionId 用于保存回复
        return travelService.chat(message, userId, sessionId);
    }

    // 获取会话列表
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> sessions = chatMessageService.getSessions(userId);
        return Result.ok(sessions);
    }

    // 获取某会话的消息列表
    @GetMapping("/messages")
    public Result<List<ChatMessage>> messages(HttpServletRequest request,
                                              @RequestParam String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatMessage> messages = chatMessageService.getMessages(userId, sessionId);
        return Result.ok(messages);
    }

    // 删除会话
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(HttpServletRequest request,
                                      @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.deleteSession(userId, sessionId);
        return Result.ok();
    }
}
```

### 13.3 FavoriteController.java

在 `src/main/java/com/whoa/controller/` 下创建：

```java
package com.whoa.controller;

import com.whoa.dto.FavoriteDTO;
import com.whoa.entity.Favorite;
import com.whoa.service.FavoriteService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 添加收藏
    @PostMapping
    public Result<Void> add(@Valid @RequestBody FavoriteDTO favoriteDTO,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.addFavorite(
                userId,
                favoriteDTO.getType(),
                favoriteDTO.getTitle(),
                favoriteDTO.getContent()
        );
        return Result.ok();
    }

    // 收藏列表（可选按类型筛选）
    @GetMapping
    public Result<List<Favorite>> list(HttpServletRequest request,
                                       @RequestParam(required = false) String type) {
        Long userId = (Long) request.getAttribute("userId");
        List<Favorite> favorites = favoriteService.listFavorites(userId, type);
        return Result.ok(favorites);
    }

    // 取消收藏
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.removeFavorite(userId, id);
        return Result.ok();
    }
}
```

---

## 第14步：改造 TravelService.java

这是最关键的一步。需要修改 `chat()` 方法，增加保存 assistant 回复的功能。

### 改造思路

当前 `chat()` 方法接收 `message` 一个参数，流式发送后结束。改造后：

1. 方法签名变为 `chat(String message, Long userId, String sessionId)`
2. 在流式回调中用 `StringBuilder` 拼装完整回复
3. 流完成后，调用 `chatMessageService.saveMessage()` 保存 assistant 的完整回复
4. 需要 `@Autowired` 注入 `ChatMessageService`

### 改造后的 TravelService.java

```java
package com.whoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoa.service.ChatMessageService;
import com.whoa.utils.LLMUtils;
import com.whoa.vo.StreamChunkVO;
import com.whoa.vo.StreamDoneVO;
import com.whoa.vo.StreamErrorVO;
import com.whoa.vo.TravelRecommendVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TravelService {
    @Value("${spring.ai.openai.api-key}")
    private String apikey;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.chat.model}")
    private String model;

    private LLMUtils llmUtils;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatMessageService chatMessageService;   // ← 新增注入

    @PostConstruct
    public void init() {
        this.llmUtils = new LLMUtils(apikey, baseUrl, model);
    }

    public TravelRecommendVO recommend(String city, Integer days, Double budget) {
        TravelRecommendVO result = new TravelRecommendVO();
        String prompt = buildTravelPrompt(city, days, budget);
        try {
            String response = llmUtils.chat(null, prompt);
            return parseTravelResponse(response);
        } catch (Exception e) {
            log.error("旅游推荐失败", e);
            result.setSuccess(false);
            result.setError("旅游推荐失败");
            return result;
        }
    }

    // 改造后的 chat 方法
    public SseEmitter chat(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(180000L);

        new Thread(() -> {
            // 用 StringBuilder 拼装完整的 assistant 回复
            StringBuilder fullContent = new StringBuilder();

            try {
                String systemPrompt = "你是一个友好的旅游助手，请用中文回答用户关于旅游的问题";
                Consumer<String> callback = content -> {
                    try {
                        fullContent.append(content);   // ← 拼装完整内容
                        String json = objectMapper.writeValueAsString(StreamChunkVO.of(content));
                        emitter.send(SseEmitter.event().data(json));
                    } catch (Exception e) {
                        log.error("发送消息失败", e);
                    }
                };
                llmUtils.chatStream(systemPrompt, message, callback);

                // 流完成后保存 assistant 回复
                chatMessageService.saveMessage(userId, sessionId, "assistant", fullContent.toString());
                log.info("对话回复已保存: userId={}, sessionId={}", userId, sessionId);

                String doneJson = objectMapper.writeValueAsString(StreamDoneVO.of());
                emitter.send(SseEmitter.event().data(doneJson));
                emitter.complete();
            } catch (Exception e) {
                log.error("对话异常", e);
                try {
                    String errorJson = objectMapper.writeValueAsString(StreamErrorVO.of(e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorJson));
                } catch (Exception e1) {
                    log.error("发送错误消息失败", e1);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    // 以下方法保持不变
    private TravelRecommendVO parseTravelResponse(String response) {
        TravelRecommendVO result = new TravelRecommendVO();
        try {
            String jsonContent = extractJson(response);
            if (jsonContent != null) {
                result = objectMapper.readValue(jsonContent, TravelRecommendVO.class);
            } else {
                result.setSuccess(false);
                result.setError("未能从响应中提取JSON");
                result.setRawResponse(response);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("JSON解析错误");
            result.setRawResponse(response);
        }
        return result;
    }

    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String buildTravelPrompt(String city, Integer days, Double budget) {
        return "你是一个专业的旅游规划师,请根据用户的需求生成详细的旅行行程。\n\n" +
                "请根据以下信息为用户生成一份详细的旅游规划:\n" +
                "- 目的地城市:" + city + "\n" +
                "-预算:" + budget + "元\n" +
                "-旅行天数:" + days + "天\n\n" +
                "要求:\n" +
                "1. 每天的行程安排(上午、下午、晚上)\n" +
                "2. 每个景点的详细介绍\n" +
                "3. 交通建议\n" +
                "4. 预算分配明细\n" +
                "5. 注意事项\n\n" +
                "请以JSON格式输出,结构如下:\n" +
                "{\"success\":true,\"city\":\"城市名\",\"days\":天数,\"totalBudget\":总预算," +
                "\"dailyItinerary\":[{\"day\":1,\"date\":\"第1天\"," +
                "\"morning\":{\"spot\":\"景点名称\",\"duration\":\"游览时长\",\"ticket\":\"门票价格\"," +
                "\"transportation\":\"交通方式\",\"description\":\"景点介绍\"}," +
                "\"afternoon\":{\"spot\":\"景点名称\",\"duration\":\"游览时长\",\"ticket\":\"门票价格\"," +
                "\"transportation\":\"交通方式\",\"description\":\"景点介绍\"}," +
                "\"evening\":{\"spot\":\"活动名称\",\"duration\":\"活动时长\",\"ticket\":\"费用\"," +
                "\"transportation\":\"交通方式\",\"description\":\"活动介绍\"}}]," +
                "\"budgetBreakdown\":{\"accommodation\":住宿费用,\"food\":餐饮费用," +
                "\"transportation\":交通费用,\"tickets\":门票费用,\"other\":其他费用}," +
                "\"tips\":[\"提示1\",\"提示2\",\"提示3\"],\"warnings\":[\"注意事项1\",\"注意事项2\"]}\n\n" +
                "请确保JSON格式正确,可以被解析。";
    }
}
```

### 改了哪些地方（对比原版）

| 改动 | 说明 |
|------|------|
| 加了 `@Slf4j` | 替换原来的 `System.out.println` |
| 加了 `@Autowired ChatMessageService` | 用于保存对话记录 |
| `chat()` 方法签名 | 从 `chat(String message)` → `chat(String message, Long userId, String sessionId)` |
| 加了 `StringBuilder fullContent` | 在回调中拼装完整回复 |
| 流完成后调 `chatMessageService.saveMessage()` | 保存 assistant 消息 |
| 所有 `System.out.println` | 换成 `log.error()` |

---

## 第15步：修改 TravelController.java

原有的 `/api/travel/chat` 接口要改，因为 `chat()` 方法签名变了。同时这个接口也需要登录了。

有两种做法：

**做法 A（推荐）**：删掉 TravelController 中的 `/chat` 接口，对话统一走 ChatController 的 `/api/chat/send`。

**做法 B**：保留 TravelController 中的 chat 接口，但改造参数。

这里推荐做法 A。改造后的 TravelController：

```java
package com.whoa.controller;

import com.whoa.dto.TravelRequestDTO;
import com.whoa.service.TravelService;
import com.whoa.vo.Result;
import com.whoa.vo.TravelRecommendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok();
    }

    @PostMapping("/recommend")
    public Result<TravelRecommendVO> recommend(@Valid @RequestBody TravelRequestDTO travelRequestDTO) {
        log.info("旅游推荐请求: city={}, days={}, budget={}",
                travelRequestDTO.getCity(), travelRequestDTO.getDays(), travelRequestDTO.getBudget());
        TravelRecommendVO travelRecommendVO = travelService.recommend(
                travelRequestDTO.getCity(),
                travelRequestDTO.getDays(),
                travelRequestDTO.getBudget()
        );
        return Result.ok(travelRecommendVO);
    }

    // 删掉了原来的 /chat 接口，对话统一走 /api/chat/send
}
```

---

## 第16步：修改 GlobalExceptionHandler

增加对 RuntimeException 的处理，因为 Service 层抛的是 RuntimeException：

```java
package com.whoa.common;

import com.whoa.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handlerException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(400, message);
    }

    // ← 新增：处理 Service 层抛出的 RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    // ← 新增：兜底处理其他异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

---

## 第17步：最终文件清单

做完以上所有步骤后，你的项目结构应该是：

```
src/main/java/com/whoa/
├── TravelJavaApplication.java          ← 改：加了 @MapperScan
├── common/
│   └── GlobalExceptionHandler.java     ← 改：加了两个异常处理
├── config/
│   ├── JwtProperties.java              ← 新增
│   └── WebMvcConfig.java               ← 新增
├── controller/
│   ├── ChatController.java             ← 新增
│   ├── FavoriteController.java         ← 新增
│   ├── TravelController.java           ← 改：删了 /chat，加了日志
│   └── UserController.java             ← 新增
├── dto/
│   ├── ChatRequestDTO.java             ← 改：加了 sessionId
│   ├── FavoriteDTO.java                ← 新增
│   ├── LoginDTO.java                   ← 新增
│   ├── RegisterDTO.java                ← 新增
│   └── TravelRequestDTO.java           ← 不变
├── entity/
│   ├── ChatMessage.java                ← 新增
│   ├── Favorite.java                   ← 新增
│   └── User.java                       ← 新增
├── interceptor/
│   └── JwtInterceptor.java             ← 新增
├── mapper/
│   ├── ChatMessageMapper.java          ← 新增
│   ├── FavoriteMapper.java             ← 新增
│   └── UserMapper.java                 ← 新增
├── service/
│   ├── ChatMessageService.java         ← 新增
│   ├── FavoriteService.java            ← 新增
│   ├── TravelService.java              ← 改：加了对话保存
│   └── UserService.java                ← 新增
├── utils/
│   ├── JwtUtils.java                   ← 新增
│   ├── LLMUtils.java                   ← 不变
│   └── PasswordUtils.java              ← 新增
└── vo/
    ├── Result.java                     ← 不变
    ├── StreamChunkVO.java              ← 不变
    ├── StreamDoneVO.java               ← 不变
    ├── StreamErrorVO.java              ← 不变
    └── TravelRecommendVO.java          ← 不变

src/main/resources/
├── application.yaml                    ← 改：加了日志配置
└── mapper/
    ├── ChatMessageMapper.xml           ← 新增
    ├── FavoriteMapper.xml              ← 新增
    └── UserMapper.xml                  ← 新增
```

---

## 第18步：测试顺序

按以下顺序用 Apifox / Postman 测试：

### 1. 注册
```
POST http://localhost:3200/api/user/register
Body: {"username": "test", "password": "123456", "nickname": "测试用户"}
```
→ 返回 token，复制保存

### 2. 登录
```
POST http://localhost:3200/api/user/login
Body: {"username": "test", "password": "123456"}
```
→ 返回 token

### 3. 获取用户信息（需要 token）
```
GET http://localhost:3200/api/user/info
Header: Authorization: Bearer <上一步的token>
```
→ 返回用户信息（password 为 null）

### 4. 发起对话（需要 token）
```
POST http://localhost:3200/api/chat/send
Header: Authorization: Bearer <token>
Body: {"sessionId": "随便一个UUID", "message": "北京有什么好玩的地方？"}
```
→ 返回 SSE 流

### 5. 查看会话列表
```
GET http://localhost:3200/api/chat/sessions
Header: Authorization: Bearer <token>
```

### 6. 查看某会话消息
```
GET http://localhost:3200/api/chat/messages?sessionId=你刚才的UUID
Header: Authorization: Bearer <token>
```

### 7. 添加收藏
```
POST http://localhost:3200/api/favorite
Header: Authorization: Bearer <token>
Body: {"type": "message", "title": "北京旅游推荐", "content": "北京有故宫、长城..."}
```

### 8. 查看收藏列表
```
GET http://localhost:3200/api/favorite
Header: Authorization: Bearer <token>
```

### 9. 不带 token 访问受保护接口
```
GET http://localhost:3200/api/user/info
```
→ 应返回 401 "未登录"

---

## 常见问题

**Q: 启动报错 "Failed to configure a DataSource"？**
A: 检查 MySQL 是否启动、数据库 `travel_ai` 是否创建、yaml 中密码是否正确。

**Q: MyBatis 报错 "Invalid bound statement"？**
A: 检查 XML 的 namespace 是否和 Mapper 接口全限定名一致，XML 文件是否在 `resources/mapper/` 下。

**Q: JWT 报错 "Secret key too short"？**
A: jwt.secret 至少 32 个字符，改长一点。

**Q: 注册时密码存的是明文？**
A: 检查 UserService.register() 中是否调用了 `PasswordUtils.hashPassword()`。

**Q: 对话接口返回 401？**
A: 检查请求 Header 是否加了 `Authorization: Bearer <token>`，token 前面要有 "Bearer " 和一个空格。
