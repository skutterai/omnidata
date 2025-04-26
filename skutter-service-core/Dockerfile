# Build stage
FROM eclipse-temurin:17-jdk-jammy as build
WORKDIR /workspace/app

# Copy the build files
COPY build.gradle ./
COPY ../gradlew ./gradlew
COPY ../gradle ./gradle

# Copy the source code
COPY src ./src

# Build the application
RUN chmod +x ./gradlew && ./gradlew build -x test

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the port the app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 