# =========================================================================
# Stage 1: Build the application using Maven and Java 17
# =========================================================================
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copy pom.xml first to fetch and cache dependencies
COPY Project/pom.xml ./
RUN mvn dependency:go-offline -B

# 2. Copy the actual source code
COPY Project/src ./src

# 3. Build the application and package it into a JAR file, skipping tests
RUN mvn clean package -DskipTests

# =========================================================================
# Stage 2: Create the lightweight, secure runtime image
# =========================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled JAR file from the build stage safely using a wildcard match
COPY --from=build /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
