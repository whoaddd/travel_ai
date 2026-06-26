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