# 使用 JDK 17 作为基础镜像
FROM openjdk:17-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建文件
COPY pom.xml .
COPY src ./src

# 安装 Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# 构建应用
RUN mvn clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "target/blog-backend-1.0.0.jar", "--spring.profiles.active=prod"]
