# =========================================================================
# Stage 1: Build the application using Maven and Java 17
# =========================================================================
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and source code from your inner "Project" directory
COPY Project/pom.xml ./
COPY Project/src ./src

# Build the application and package it into a JAR file, skipping tests
RUN mvn clean package -DskipTests

# =========================================================================
# Stage 2: Create the lightweight, secure runtime image
# =========================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
