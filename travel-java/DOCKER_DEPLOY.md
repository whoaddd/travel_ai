# AI旅行助手 - Docker 部署文档

> 将后端、前端、MySQL 全部打包到 VMware Ubuntu 虚拟机中使用 Docker 运行。

---

## 一、环境准备

### 1.1 Ubuntu 虚拟机要求

- Ubuntu 20.04 LTS 或 22.04 LTS
- 至少 4GB 内存
- 已安装 Docker 和 Docker Compose

### 1.2 安装 Docker（如果未安装）

```bash
# 更新包索引
sudo apt update

# 安装依赖
sudo apt install -y ca-certificates curl gnupg lsb-release

# 添加 Docker GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 添加 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 添加当前用户到 docker 组（免 sudo）
sudo usermod -aG docker $whoami
# 重新登录后生效
```

---

## 二、项目文件结构

在 Ubuntu 虚拟机上创建项目目录：

```bash
mkdir -p /opt/travel-ai
cd /opt/travel-ai
```

目录结构：

```
/opt/travel-ai/
├── docker-compose.yml          # 主编排文件
├── backend/
│   ├── Dockerfile              # 后端 Docker 镜像
│   └── target/
│       └── travel-java-0.0.1-SNAPSHOT.jar   # 后端 jar 包
├── frontend/
│   ├── Dockerfile              # 前端 Docker 镜像
│   ├── nginx.conf              # Nginx 配置
│   └── dist/                   # 前端构建产物（需从本地复制）
└── mysql/
    └── init/                   # MySQL 初始化脚本
        └── 01-init.sql
```

---

## 三、后端打包（本地 Windows 操作）

### 3.1 打包后端 JAR

在 Windows 上进入后端项目目录，执行：

```bash
cd D:\Code\project\travel-java
mvn clean package -DskipTests
```

打包成功后，在 `target/` 目录下找到 `travel-java-0.0.1-SNAPSHOT.jar`，将其复制到 Ubuntu 虚拟机的 `/opt/travel-ai/backend/target/` 目录。

### 3.2 打包前端

在前端项目目录执行：

```bash
cd D:\Code\project\travel-vue
npm install
npm run build
```

构建完成后，将 `dist/` 目录下的所有文件复制到 Ubuntu 虚拟机的 `/opt/travel-ai/frontend/dist/` 目录。

---

## 四、创建配置文件

### 4.1 MySQL 初始化脚本

创建文件 `/opt/travel-ai/mysql/init/01-init.sql`：

```sql
CREATE DATABASE IF NOT EXISTS travel_ai DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travel_ai;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_id` VARCHAR(36) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

### 4.2 前端 Nginx 配置

创建文件 `/opt/travel-ai/frontend/nginx.conf`：

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://backend:3200/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # SSE 流式响应需要这些头
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
    }
}
```

---

## 五、创建 Docker 镜像文件

### 5.1 后端 Dockerfile

创建文件 `/opt/travel-ai/backend/Dockerfile`：

```dockerfile
# 基础镜像
FROM eclipse-temurin:21-jdk-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 jar 包
COPY target/travel-java-0.0.1-SNAPSHOT.jar app.jar

# 运行镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/app.jar .

# 时区设置
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 暴露端口
EXPOSE 3200

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2 前端 Dockerfile

创建文件 `/opt/travel-ai/frontend/Dockerfile`：

```dockerfile
# 构建阶段（可选，如果不想在宿主机 build）
# FROM node:21-alpine AS builder
# WORKDIR /app
# COPY package.json package-lock.json ./
# RUN npm ci
# COPY . .
# RUN npm run build

# 运行阶段
FROM nginx:alpine

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义配置
COPY nginx.conf /etc/nginx/conf.d/

# 复制前端构建产物
COPY dist/ /usr/share/nginx/html/

# 暴露端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

---

## 六、创建 Docker Compose 编排文件

创建文件 `/opt/travel-ai/docker-compose.yml`：

```yaml
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: travel-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root123456
      MYSQL_DATABASE: travel_ai
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
    networks:
      - travel-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # 后端 Java 服务
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: travel-backend
    restart: always
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/travel_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123456
      TZ: Asia/Shanghai
    ports:
      - "3200:3200"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - travel-network

  # 前端 Nginx 服务
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: travel-frontend
    restart: always
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - travel-network

# 网络
networks:
  travel-network:
    driver: bridge

# 数据卷
volumes:
  mysql_data:
```

---

## 七、部署步骤

### 7.1 将文件传输到 Ubuntu 虚拟机

**方式一：使用 scp（从 Windows 命令行）**

```bash
# 复制后端 JAR
scp -r D:\Code\project\travel-java\target\travel-java-0.0.1-SNAPSHOT.jar user@ubuntu-ip:/opt/travel-ai/backend/target/

# 复制前端 dist 目录
scp -r D:\Code\project\travel-vue\dist\* user@ubuntu-ip:/opt/travel-ai/frontend/dist/
```

**方式二：使用 Git 仓库（推荐）**

```bash
# 在 Ubuntu 上克隆仓库
cd /opt
git clone <your-repo-url> travel-ai
cd travel-ai

# 然后在 Windows 上 push，Ubuntu 上 pull
```

### 7.2 在 Ubuntu 上构建并启动

```bash
cd /opt/travel-ai

# 构建并启动所有容器
docker compose up -d --build

# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 7.3 验证服务

```bash
# MySQL
curl http://localhost:3306

# 后端 API
curl http://localhost:3200/api/travel/hello

# 前端
curl http://localhost:80
```

---

## 八、常用操作命令

### 8.1 启动/停止/重启

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启某个服务
docker compose restart backend
```

### 8.2 查看日志

```bash
# 查看所有日志
docker compose logs -f

# 查看某个服务日志
docker compose logs -f backend
docker compose logs -f mysql
```

### 8.3 进入容器

```bash
# 进入 MySQL 容器
docker exec -it travel-mysql mysql -uroot -proot123456

# 进入后端容器（查看日志文件等）
docker exec -it travel-backend sh

# 进入前端容器
docker exec -it travel-frontend sh
```

### 8.4 数据备份与恢复

```bash
# 备份 MySQL 数据
docker exec travel-mysql mysqldump -uroot -proot123456 travel_ai > backup.sql

# 恢复 MySQL 数据
docker exec -i travel-mysql mysql -uroot -proot123456 travel_ai < backup.sql
```

---

## 九、常见问题

### 9.1 容器启动失败

```bash
# 查看具体错误
docker compose logs service-name

# 例如查看后端错误
docker compose logs backend
```

### 9.2 前端无法访问后端 API

检查：
1. 后端是否启动成功：`docker compose ps`
2. 前端 nginx 配置是否正确代理到 `http://backend:3200/`
3. 检查前端容器日志：`docker compose logs frontend`

### 9.3 MySQL 连接失败

1. 确认 MySQL 容器已启动：`docker compose ps`
2. 等待 MySQL 完全启动（healthcheck 通过）
3. 检查后端配置中的数据库密码是否正确

### 9.4 修改代码后重新部署

```bash
# 1. 重新打包后端（本地 Windows）
mvn clean package -DskipTests

# 2. 重新打包前端
npm run build

# 3. 复制文件到 Ubuntu
scp target/travel-java-0.0.1-SNAPSHOT.jar user@vm:/opt/travel-ai/backend/target/
scp -r dist/* user@vm:/opt/travel-ai/frontend/dist/

# 4. 重新构建并启动
docker compose up -d --build
```

### 9.5 数据持久化

当前配���中：
- MySQL 数据保存在 `mysql_data` 卷中，容器重启后数据不会丢失
- 如需完全删除数据：`docker compose down -v`

---

## 十、生产环境建议

1. **修改默认密码**：将 `docker-compose.yml` 中的 MySQL 密码改掉
2. **添加 HTTPS**：使用 Let's Encrypt 或购买 SSL 证书
3. **数据备份**：定期备份 MySQL 数据
4. **监控**：使用 Prometheus + Grafana 监控容器状态
5. **日志收集**：使用 ELK 或 Loki 收集容器日志