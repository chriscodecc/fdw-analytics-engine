# --- STAGE 1: Builder ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the Spring Boot executable JAR
RUN mvn clean package -DskipTests

# --- STAGE 2: Runtime (Minimal) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create the non-root user in the Alpine way
RUN addgroup -g 1000 vscode \
    && adduser -u 1000 -G vscode -D vscode

# Copy the built JAR from Stage 1 using a wildcard (handles version changes automatically)
# and rename it to app.jar
COPY --from=build /app/target/*.jar /app/app.jar

# Give the non-root user ownership of the jar
RUN chown vscode:vscode /app/app.jar

# Drop privileges to the non-root user
USER vscode

# Standard Spring Boot execution
ENTRYPOINT ["java", "-jar", "/app/app.jar"]