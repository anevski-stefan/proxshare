FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B

COPY src ./src
EXPOSE 8080
CMD ["./mvnw", "spring-boot:run"]
