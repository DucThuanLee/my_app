# syntax=docker/dockerfile:1.7

# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Tối ưu cache dependency
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Build jar
COPY src ./src
RUN mvn -q -DskipTests clean package

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user non-root
RUN addgroup -S app && adduser -S app -G app
USER app

# Copy jar (nếu bạn dùng spring-boot-maven-plugin thì jar thường là *-SNAPSHOT.jar)
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
