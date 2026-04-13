FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace
COPY backend/mvnw ./mvnw
COPY backend/.mvn ./.mvn

COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY backend/pom.xml ./backend/pom.xml
COPY worker/pom.xml ./worker/pom.xml

RUN chmod +x mvnw \
    && ./mvnw -f pom.xml -pl backend -am dependency:go-offline -B

COPY common/src ./common/src
COPY backend/src ./backend/src

RUN ./mvnw -f pom.xml -pl backend -am package -DskipTests -B

FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app
COPY --from=build /workspace/backend/target/backend-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
