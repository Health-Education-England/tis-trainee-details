FROM openjdk:11-slim
EXPOSE 8203
COPY build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
