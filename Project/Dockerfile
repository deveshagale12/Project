# Stage 1: Build the application using Maven
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the jar file, skipping tests for faster deployment
RUN mvn clean package -DskipTests

# Stage 2: Run the application using a slim Java runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose the port your app runs on
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
