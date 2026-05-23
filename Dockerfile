# STAGE 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests here because we already know they pass!
RUN mvn clean package -DskipTests

# STAGE 2: Create the lightweight production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy only the compiled JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]