# --- STAGE 1: Builder ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
# Build the Spring Boot executable JAR
RUN mvn clean package -DskipTests

# --- STAGE 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create the non-root user in the Alpine way
RUN addgroup -g 1000 vscode \
    && adduser -u 1000 -G vscode -D vscode

# Strips away the root user to user:group 
COPY --chown=vscode:vscode --from=build /app/target/*.jar /app/app.jar

# Drop privileges to the non-root user
USER vscode

# Standard Spring Boot execution
ENTRYPOINT ["java", "-jar", "/app/app.jar"]