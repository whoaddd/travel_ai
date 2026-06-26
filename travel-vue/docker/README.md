# Docker 部署指南

## 前置要求

- VMware 虚拟机
- Docker 和 Docker Compose 已安装
- 至少 4GB 内存可用

## 快速开始

### 1. 确保 Docker 已安装并运行

```bash
# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker compose version
```

### 2. 启动所有服务

```bash
# 进入部署目录
cd docker

# 启动所有服务（首次构建可能需要几分钟）
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 3. 访问应用

- 前端页面：http://localhost
- 后端 API：http://localhost:3200
- MySQL：localhost:3306（用户名：root，密码：123456）

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| travel-vue | 80 | 前端 Nginx（Vue 生产构建） |
| travel-java | 3200 | 后端 Spring Boot API |
| mysql | 3306 | MySQL 8.0 数据库 |

## 常用命令

```bash
# 停止所有服务
docker compose down

# 重新构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f travel-java
docker compose logs -f travel-vue

# 进入容器（调试用）
docker exec -it travel-java sh
docker exec -it travel-mysql mysql -uroot -p123456 travel_ai
```

## 数据持久化

- MySQL 数据存储在 Docker 卷中，删除容器不会丢失数据
- 如需重置数据库：
  ```bash
  docker compose down -v
  docker compose up -d --build
  ```

## 目录结构

```
├── docker/
│   ├── docker-compose.yml     # Docker 编排配置
│   ├── nginx/
│   │   └── default.conf       # Nginx 配置（API 代理）
│   └── mysql/
│       └── init.sql           # 数据库初始化脚本
├── Dockerfile                 # 前端构建文件
├── ../Dockerfile             # 后端构建文件
└── README.md                 # 本文件
```