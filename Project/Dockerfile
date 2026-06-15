# =========================================================================
# Stage 1: Build the application using Maven
# =========================================================================
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copy the parent-level configuration if any, but specifically target your subfolder
# We copy the pom.xml and src directory from your nested "Project" directory
COPY Project/pom.xml ./
COPY Project/src ./src

# 2. Build the application and package it into a JAR file
# We skip tests to speed up the deployment build process
RUN mvn clean package -DskipTests

# =========================================================================
# Stage 2: Create the lightweight runtime image
# =========================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 3. Copy the compiled JAR file from the build stage
# Instead of using a wildcard (*.jar) which can cause conflicts, 
# this targets the fat JAR built by Maven in the target folder.
COPY --from=build /app/target/*.jar app.jar

# 4. Expose the port your Spring Boot application listens on (default is 8080)
EXPOSE 8080

# 5. Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
