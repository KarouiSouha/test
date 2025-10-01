# Use OpenJDK 17 as base image
FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Copy the pre-built JAR file
COPY target/health-auth-service-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Expose the port your app runs on
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]