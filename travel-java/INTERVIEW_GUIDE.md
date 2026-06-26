# AI旅行助手 - 面试项目详解

> 完整讲解项目的每一行代码，帮助你在面试中深入阐述项目架构和技术细节。

---

## 一、项目概述

### 1.1 项目定位

这是一个 **AI 驱动的旅行助手 Web 应用**，用户可以：
- 根据目的地、天数、预算生成详细的旅行行程规划
- 与 AI 进行实时对话，获取旅游相关问题的解答
- 用户登录注册，保存对话历史
- 收藏喜欢的行程或对话

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 4.1 + Java 21 |
| AI | 智谱 GLM-5.1 大模型 |
| 数据库 | MySQL 8.0 + MyBatis |
| 安全 | JWT 鉴权 + BCrypt 密码加密 |
| 前端 | Vue 3 + Vite + Axios |
| 部署 | Docker + Docker Compose |

---

## 二、后端项目结构详解

### 2.1 启动类

**文件**: `TravelJavaApplication.java`

```java
@SpringBootApplication                  // Spring Boot 启动注解，自动配置
@MapperScan("com.whoa.mapper")          // MyBatis Mapper 接口扫描路径
public class TravelJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelJavaApplication.class, args);  // 启动Spring Boot应用
    }
}
```

**面试话术**：
> 项目的启动类，使用 @SpringBootApplication 启用自动配置。@MapperScan 指定了 MyBatis Mapper 接口的包路径，这样 MyBatis 会自动扫描并注入这些 Mapper Bean，无需每个接口都加 @Mapper 注解。

---

### 2.2 配置类

#### 2.2.1 JWT 配置属性

**文件**: `JwtProperties.java`

```java
@Data
@Component
@ConfigurationProperties(prefix = "jwt")  // 读取 yaml 中 jwt 开头的配置
public class JwtProperties {
    private String secret;     // JWT 签名密钥
    private Long expiration;   // Token 过期时间（毫秒）
}
```

**面试话术**：
> 使用 @ConfigurationProperties 实现配置绑定，将 application.yaml 中 jwt.secret 和 jwt.expiration 自动注入到这个 Bean 中。这种方式比 @Value 更类型安全，支持复杂配置对象的绑定。

---

#### 2.2.2 MyBatis 配置

**文件**: `MyBatisConfig.java`

```java
@Configuration
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);                    // 设置数据源
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath:mapper/*.xml"));              // 指定 Mapper XML 位置
        factoryBean.setTypeAliasesPackage("com.whoa.entity");      // 实体类包路径
        
        // 开启驼峰命名转换：user_id -> userId
        org.apache.ibatis.session.Configuration configuration = 
            new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        
        return factoryBean.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);      // 配置事务管理器
    }
}
```

**面试话术**：
> Spring Boot 4.x 版本对 MyBatis 的自动配置有变化，这里手动配置 SqlSessionFactory。关键是开启 `mapUnderscoreToCamelCase: true`，这样数据库的 user_id 字段会自动映射到 Java 的 userId 属性，省去手动配置 resultMap 的麻烦。

---

#### 2.2.3 Web MVC 配置 + 拦截器注册

**文件**: `WebMvcConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")                    // 拦截所有 /api 开头的请求
                .excludePathPatterns(
                        "/api/user/login",                      // 登录接口不拦截
                        "/api/user/register",                   // 注册接口不拦截
                        "/api/travel/hello"                     // 健康检查不拦截
                );
    }

    // 配置跨域
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")                     // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);                                   // 预检请求缓存1小时
    }
}
```

**面试话术**：
> 这里做了两件事。第一是注册 JWT 拦截器，所有 /api/** 的请求都会被拦截检查 token，只有登录和注册接口被排除。第二是配置 CORS 跨域，因为前后端分离部署时，浏览器会阻止不同源的请求。allowedOriginPatterns 使用通配符比 allowedOrigins 更灵活。

---

### 2.3 实体类（Entity）

#### 2.3.1 User 实体

```java
@Data
public class User {
    private Long id;           // 主键
    private String username;   // 用户名（唯一）
    private String password;   // BCrypt 加密后的密码
    private String nickname;   // 昵称
    private String avatar;     // 头像URL
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}
```

**面试话术**：
> 这是用户实体类，对应数据库的 user 表。使用 Lombok 的 @Data 自动生成 getter/setter/toString 等方法。createdAt 和 updatedAt 字段会在数据库层面自动管理，创建时默认当前时间，更新时自动更新时间。

---

#### 2.3.2 ChatMessage 实体

```java
@Data
public class ChatMessage {
    private Long id;           // 主键
    private Long userId;       // 关联用户ID
    private String sessionId;  // 会话ID（同一轮对话共享一个sessionId）
    private String role;       // 角色：user 或 assistant
    private String content;    // 消息内容
    private LocalDateTime createdAt;  // 创建时间
}
```

**面试话术**：
> 对话消息实体。sessionId 是为了支持多会话设计的，同一用户可以开启多个对话，每个对话有独立的 sessionId。role 字段区分是用户发的消息还是 AI 的回复，这样可以在前端按会话展示完整对话历史。

---

#### 2.3.3 Favorite 实体

```java
@Data
public class Favorite {
    private Long id;           // 主键
    private Long userId;       // 关联用户ID
    private String type;       // 收藏类型：itinerary（行程）/ message（消息）
    private String title;      // 收藏标题
    private String content;    // 收藏内容（JSON 字符串存储）
    private LocalDateTime createdAt;
}
```

**面试话术**：
> 收藏实体，支持两种类型。用户可以收藏 AI 生成的行程规划，也可以收藏某条对话消息。content ���段存储的是序列化后的 JSON 字符串，这样可以保存复杂的数据结构。

---

### 2.4 Mapper 层

#### 2.4.1 UserMapper

```java
@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);   // 登录时按用户名查询
    void insert(User user);                                     // 注册时插入用户
    User findById(@Param("id") Long id);                        // 查询用户信息
}
```

**对应 XML**:

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (username, password, nickname, avatar)
    VALUES (#{username}, #{password}, #{nickname}, #{avatar})
</insert>
```

**面试话术**：
> Mapper 接口使用 @Mapper 注解标记，MyBatis 会自动实现。useGeneratedKeys="true" keyProperty="id" 表示插入后自动回填生成的主键到 user 对象中，这样注册完成后可以直接拿到用户的 id。

---

### 2.5 工具类

#### 2.5.1 JwtUtils - JWT 生成与解析

```java
@Component
public class JwtUtils {
    private final JwtProperties jwtProperties;

    // 生成 Token
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)                              // 设置主题（用户名）
                .claim("userId", userId)                         // 放入用户ID payload
                .issuedAt(new Date())                            // 签发时间
                .expiration(new Date(System.currentTimeMillis() 
                    + jwtProperties.getExpiration()))            // 过期时间
                .signWith(getKey())                              // 签名
                .compact();
    }

    // 解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 从 Token 中获取 userId
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }
}
```

**面试话术**：
> JWT 工具类使用 jjwt 库。generateToken 生成 token 时放入 subject（用户名）和 claim（userId）两个信息。parseToken 解析并验证签名，验证通过才返回 payload。getKey() 方法将配置的 secret 密钥转换为特定算法需要的 Key 对象。Token 过期时间从配置文件中读取，默认为 24 小时。

---

#### 2.5.2 PasswordUtils - 密码加密

```java
public class PasswordUtils {
    // 加密密码：使用 BCrypt 强哈希算法
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // 校验密码
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
```

**面试话术**：
> 密码加密使用 BCrypt 算法，这是业界推荐的密码哈希方案。BCrypt 会自动加盐，每次生成的哈希值都不同，防止彩虹表攻击。hashpw 内部默认使用 10 轮加密迭代，兼顾安全性和性能。校验时使用 checkpw，它会提取存储的哈希值中的盐，然后对明文密码进行相同轮次的哈希，最后比对结果。

---

#### 2.5.3 LLMUtils - 大模型调用工具

```java
public class LLMUtils {
    private OkHttpClient client;

    // 同步调用（非流式）
    public String chat(String systemPrompt, String userPrompt) {
        String requestBody = buildRequestBody(systemPrompt, userPrompt, false);
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return extractContent(responseBody);
        }
    }

    // 流式调用
    public String chatStream(String systemPrompt, String userPrompt, 
                             Consumer<String> callback) {
        // 关键：设置 Accept: text/event-stream 表示接受流式响应
        Request request = new Request.Builder()
                .addHeader("Accept", "text/event-stream")
                // ... 其他配置
                
        try (Response response = client.newCall(request).execute()) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8)
            );
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5);
                    if ("[DONE]".equals(data)) break;  // 流结束标志
                    String content = parseStreamContent(data);
                    if (content != null) {
                        fullContent.append(content);
                        callback.accept(content);  // 回调发送每个 chunk
                    }
                }
            }
        }
    }
}
```

**面试话术**：
> LLMUtils 是封装大模型调用的核心工具类。chat() 是同步调用，一次性获取完整响应，适用于旅行推荐场景。chatStream() 是流式调用，通过 SSE（Server-Sent Events）实时返回每个 token，实现打字机效果。关键点：1）Accept 头要设为 text/event-stream；2）响应体是流式的，以 "data:" 开头；3）"[DONE]" 是结束标志；4）每个 chunk 都通过 callback 回调给调用方。

---

### 2.6 拦截器

#### 2.6.1 JwtInterceptor - Token 验证拦截器

```java
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, Object handler) {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            writeError(response, 401, "未登录");
            return false;
        }

        token = token.substring(7);  // 去掉 "Bearer " 前缀

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 存入 request 属性，后续 Controller 可直接获取
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            return true;
        } catch (ExpiredJwtException e) {
            writeError(response, 401, "登录已过期");
            return false;
        }
    }
}
```

**面试话术**：
> JWT 拦截器是整个应用的安全核心。在请求到达 Controller 之前拦截，检查 Token 是否有效。关键设计：1）放行 OPTIONS 请求，这是浏览器 CORS 预检请求；2）从 Header 中获取 Authorization 字段，格式为 "Bearer token"；3）解析成功后将 userId 存入 request.setAttribute，这样在 Controller 中可以通过 request.getAttribute("userId") 获取当前用户 ID，而不是让前端传递（防止伪造）；4）统一返回 401 错误码给前端。

---

### 2.7 Service 层

#### 2.7.1 TravelService - 旅行推荐和对话服务

```java
@Slf4j
@Service
public class TravelService {
    @Value("${spring.ai.openai.api-key}")
    private String apikey;

    @PostConstruct
    public void init() {
        this.llmUtils = new LLMUtils(apikey, baseUrl, model);  // 初始化 LLM 工具
    }

    // 同步旅行推荐
    public TravelRecommendVO recommend(String city, Integer days, Double budget) {
        String prompt = buildTravelPrompt(city, days, budget);  // 构建 Prompt
        try {
            String response = llmUtils.chat(null, prompt);       // 调用大模型
            return parseTravelResponse(response);                // 解析 JSON
        } catch (Exception e) {
            log.error("旅游推荐失败", e);
            // 返回错误结果而不是抛出异常
            result.setSuccess(false);
            result.setError("旅游推荐失败");
            return result;
        }
    }

    // 流式对话
    public SseEmitter chat(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(180000L);  // 180秒超时
        
        new Thread(() -> {
            StringBuilder fullContent = new StringBuilder();
            try {
                Consumer<String> callback = content -> {
                    fullContent.append(content);  // 拼装完整回复
                    // 发送 SSE 事件
                    emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(StreamChunkVO.of(content))));
                };
                llmUtils.chatStream(systemPrompt, message, callback);
                
                // 流结束后保存 assistant 回复到数据库
                chatMessageService.saveMessage(userId, sessionId, "assistant", 
                    fullContent.toString());
                
                emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(StreamDoneVO.of())));
                emitter.complete();
            } catch (Exception e) {
                // 发送错误事件
                emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(StreamErrorVO.of(e.getMessage()))));
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    // 构建旅行规划 Prompt
    private String buildTravelPrompt(String city, Integer days, Double budget) {
        // 详细的 Prompt 工程，指定输出 JSON 格式和结构
        return "你是一个专业的旅游规划师...";
    }

    // 提取并解析 JSON
    private TravelRecommendVO parseTravelResponse(String response) {
        String jsonContent = extractJson(response);  // 用正则提取 JSON
        return objectMapper.readValue(jsonContent, TravelRecommendVO.class);
    }
}
```

**面试话术**：
> TravelService 是核心业务逻辑类，包含两个主要功能：1）recommend() 同步调用大模型生成旅行规划，使用 Prompt Engineering 指定输出 JSON 格式，然后解析为 VO 对象返回给前端。2）chat() 流式对话，使用 Spring 的 SseEmitter 实现 Server-Sent Events，每个 token 通过回调实时推送给前端。关键设计：使用独立线程处理流式请求，避免阻塞主线程；流结束后将完整的 assistant 回复保存到数据库，这样用户可以查看历史对话；180秒超时设置考虑到大模型生成可能需要较长时间。

---

#### 2.7.2 UserService - 用户服务

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public String register(String username, String password, String nickname) {
        // 1. 检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 密码加密存储
        String hashedPassword = PasswordUtils.hashPassword(password);

        // 3. 插入数据库
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setNickname(nickname != null ? nickname : username);
        userMapper.insert(user);  // 插入后 user.getId() 自动回填

        // 4. 生成 Token 返回给前端
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            // 防止用户名枚举攻击
            throw new RuntimeException("用户名或密码错误");
        }

        // BCrypt 校验密码
        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    public User getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null);  // 脱敏，不返回密码
        }
        return user;
    }
}
```

**面试话术**：
> UserService 处理用户注册登录。关键点：1）注册时先检查用户名是否存在，防止重复注册；2）密码使用 BCrypt 加密后再存入数据库，绝对不能存明文；3）登录时使用统一的错误提示"用户名或密码错误"，防止攻击者通过不同提示枚举存在的用户名；4）登录成功后生成 JWT Token 返回给前端，后续请求携带此 Token；5）查询用户信息时将 password 置为 null，防止泄露。

---

### 2.8 Controller 层

#### 2.8.1 TravelController

```java
@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok();  // 健康检查接口
    }

    @PostMapping("/recommend")
    public Result<TravelRecommendVO> recommend(
            @Valid @RequestBody TravelRequestDTO dto) {  // 参数校验
        log.info("旅游推荐请求: city={}, days={}, budget={}", 
            dto.getCity(), dto.getDays(), dto.getBudget());
        
        TravelRecommendVO vo = travelService.recommend(
            dto.getCity(), dto.getDays(), dto.getBudget());
        return Result.ok(vo);
    }
}
```

**面试话术**：
> TravelController 提供旅行相关接口。@Valid 注解触发参数校验，如果 DTO 中的 @NotNull、@Min 等校验失败，会抛出异常被 GlobalExceptionHandler 捕获并返回友好错误信息。hello() 是健康检查接口，用于负载均衡探测服务是否可用。

---

#### 2.8.2 ChatController

```java
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final TravelService travelService;
    private final ChatMessageService chatMessageService;

    @PostMapping(value = "/send", produces = "text/event-stream")
    public SseEmitter chat(@Valid @RequestBody ChatRequestDTO dto,
                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");  // 从 Token 获取
        String sessionId = dto.getSessionId();
        
        // 保存用户消息
        chatMessageService.saveMessage(userId, sessionId, "user", dto.getMessage());
        
        // 调用流式对话，返回 SseEmitter
        return travelService.chat(dto.getMessage(), userId, sessionId);
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(chatMessageService.getSessions(userId));
    }
}
```

**面试话术**：
> ChatController 处理对话相关请求。关键点：1）userId 从拦截器注入的 request.getAttribute 中获取，而不是从请求体获取，确保用户无法伪造身份；2）/send 接口返回 SseEmitter，produces = "text/event-stream" 告诉 Spring 这是流式响应；3）每次对话先保存用户消息，等流结束后再保存 AI 回复；4）/sessions 接口查询用户的所有会话列表，用于前端展示历史对话。

---

#### 2.8.3 UserController

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        String token = userService.register(dto.getUsername(), 
            dto.getPassword(), dto.getNickname());
        return Result.ok(token);  // 直接返回 Token
    }

    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(userService.getUserById(userId));
    }
}
```

---

### 2.9 统一响应格式

**文件**: `Result.java`

```java
@Data
public class Result<T> {
    private Boolean success;
    private Integer code;
    private String message;
    private T data;
    private String error;
    private String rawResponse;  // 保留原始响应，用于调试

    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = ok();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = fail(message);
        result.setCode(code);
        return result;
    }
}
```

**面试话术**：
> 统一的响应格式是前后端规范接口的重要实践。所有接口都返回 Result 对象，success 表示是否成功，code 是状态码，message 是提示信息，data 是业务数据。这样前端可以统一处理所有接口响应，不需要每个接口单独判断格式。fail() 方法支持自定义错误码，便于区分不同类型的错误。

---

### 2.10 全局异常处理

**文件**: `GlobalExceptionHandler.java`

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(400, message);
    }

    // 处理业务异常
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    // 兜底处理
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

**面试话术**：
> @RestControllerAdvice 注解的类会自动处理所有 Controller 抛出的异常。第一个方法处理参数校验失败异常，将所有校验错误信息拼接成逗号分隔的字符串返回给前端。第二个方法处理业务层抛出的 RuntimeException，比如用户名已存在、密码错误等。第三个是兜底方法，捕获所有未预料的异常，防止返回 500 页面给用户，同时记录详细错误日志便于排查。

---

## 三、数据库设计

### 3.1 用户表 user

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,          -- BCrypt 加密后的密码
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
);
```

---

### 3.2 对话消息表 chat_message

```sql
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_id` VARCHAR(36) NOT NULL,         -- UUID
  `role` VARCHAR(20) NOT NULL,               -- 'user' 或 'assistant'
  `content` TEXT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
);
```

---

### 3.3 收藏表 favorite

```sql
CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL,               -- 'itinerary' 或 'message'
  `title` VARCHAR(200) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`)
);
```

---

## 四、前端项目结构

前端使用 Vue 3 + Vite，主要目录：

```
src/
├── api/
│   └── travel.js          # API 封装，包含 axios 拦截器
├── stores/
│   └── auth.js            # Pinia 状态管理，存储 token
├── views/
│   └── ...                # 页面组件
├── router/
│   └── index.js           # Vue Router 配置
├── App.vue                # 根组件
└── main.js                # 入口文件
```

### 4.1 API 封装

```javascript
// 请求拦截器：自动添加 Token
request.interceptors.request.use(config => {
    const authStore = useAuthStore()
    if (authStore.token) {
        config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
})

// 响应拦截器：统一处理 401
request.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            const authStore = useAuthStore()
            authStore.logout()  // Token 过期，清除登录状态
        }
    }
)
```

---

## 五、关键技术点面试总结

### 5.1 为什么用 JWT 而不是 Session？

| 特点 | JWT | Session |
|------|-----|---------|
| 存储位置 | 客户端（Token） | 服务端（Session） |
| 状态管理 | 无状态，服务端不存储 | 有状态，需要 Session 存储 |
| 扩展性 | 适合分布式多节点 | 需要共享 Session（如 Redis） |
| 性能 | 每次请求都要解析 | 需要查 Session 存储 |
| 适用场景 | 微服务、移动端 | 传统 Web 应用 |

**本项目选择 JWT 的原因**：
1. 前后端分离架构，不需要服务端存储 Session
2. 移动端友好，Token 可以在移动端持久化
3. 简化部署，不需要 Redis 等 Session 存储

---

### 5.2 SSE vs WebSocket

| 特点 | SSE | WebSocket |
|------|-----|-----------|
| 协议 | HTTP/HTTPS | WebSocket (ws://) |
| 方向 | 单向（服务端→客户端） | 双向 |
| 适用场景 | 实时推送、股票行情、聊天 | 实时游戏、协同编辑 |
| 防火墙 | 容易被拒绝 | 需要特殊端口 |
| 自动重连 | 支持 | 不支持 |

**本项目选择 SSE 的原因**：
1. 项目只需要单向推送（AI → 用户），不需要用户→服务端实时消息
2. SSE 基于标准 HTTP，不需要特殊协议
3. 实现简单，Spring 内置 SseEmitter 支持

---

### 5.3 MyBatis #{} vs ${}

| 特点 | #{} | ${} |
|------|-----|-----|
| 安全性 | 预编译，防止 SQL 注入 | 直接拼接，有 SQL 注入风险 |
| 性能 | 预编译，性能高 | 每次都要解析 |
| 适用场景 | 绝大多数场景 | 动态表名、排序字段 |

**面试要说的**：
> MyBatis 中优先使用 #{}，它会使用预编译语句，参数会被当作绑定变量处理，彻底防止 SQL 注入。只有在动态表名、ORDER BY 等无法使用绑定变量的场景才考虑 ${}，但需要自行做好参数校验。

---

### 5.4 BCrypt 加密原理

1. **加盐**：每次加密随机生成盐值，同一密码两次加密结果不同
2. **迭代**：默认 10 轮迭代，兼顾安全性和性能
3. **哈希算法**：基于 Blowfish 加密算法

**面试要说的**：
> BCrypt 是业界推荐的密码哈希方案。每次加密随机生成盐值，即使相同密码生成的哈希值也不同，防止攻击者使用彩虹表攻击。10 轮迭代使得暴力破解成本极高，同时在可接受范围内不影响正常登录性能。

---

## 六、项目架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                         │
│   浏览器 ──> Nginx ──> API 请求                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────��──────────��────────────────────────────────┐
│                    后端 (Spring Boot)                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Controller │───▶│   Service    │───▶│    Mapper    │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                                              │     │
│         ▼                                              ▼     │
│  ┌──────────────┐                           ┌──────────────┐ │
│  │  Interceptor │                           │   MySQL      │ │
│  │  (JWT验证)    │                           │  (数据库)     │ │
│  └──────────────┘                           └──────────────┘ │
│                                                              │
│         │                                                    │
│         ▼                                                    │
│  ┌──────────────┐                                           │
│  │  LLMUtils    │──────▶ 智谱 GLM-5.1 大模型                  │
│  └──────────────┘                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 七、面试常见问题及回答

### Q1: 项目的技术选型是怎么考虑的？

**回答**：
> 后端选择 Spring Boot 是因为它是 Java 生态最成熟的快速开发框架，配置简单且生态丰富。选择 MyBatis 而不是 JPA，是因为项目业务相对简单，MyBatis 的 SQL 控制能力更强，更适合复杂查询场景。前端选择 Vue 3 是国内生态好，文档完善且上手快。
>
> AI 接入选择智谱 GLM 是因为国内大模型中性价比高，支持 OpenAI 兼容的 API 格式，接入成本低。

---

### Q2: 如何保证用户密码安全？

**回答**：
> 三个方面：1）传输层，全站 HTTPS 加密传输；2）存储层，密码使用 BCrypt 算���加密存储，即使数据库泄露也无法还原明文；3）传输格式，Token 放在 HttpOnly Cookie 或 localStorage 中（配合 Bearer 头）。

---

### Q3: 如果遇到大并发请求，如何优化？

**回答**：
> 几个层面的优化思路：1）数据库层面，添加合理索引，避免全表扫描；2）缓存层面，可以用 Redis 缓存热门行程推荐结果；3）连接池层面，HikariCP 连接池合理配置最大连接数；4）异步化，非核心逻辑走消息队列处理。

---

### Q4: 项目中遇到的最大挑战是什么？

**回答**：
> 流式对话的实现。SSE 需要保持长连接，后端需要正确处理超时和断连场景。最初用同步方式调用导致线程阻塞，后来改用独立线程处理，同时要做好异常处理确保连接能正确关闭。另外 Nginx 默认 60 秒超时，需要手动配置代理超时时间。

---

### Q5: 项目的不足和改进方向？

**回答**：
> 当前版本的不足：1）没有实现请求限流，大并发可能冲垮服务；2）日志没有集中收集分析；3）没有实现完整的单元测试覆盖。改进方向：1）引入 Sentinel 做限流和熔断；2）接入 ELK/Loki 做日志收集；3）补充测试用例，特别是 Service 层的单元测试。