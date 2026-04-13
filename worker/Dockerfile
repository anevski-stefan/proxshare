FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace
COPY worker/mvnw ./mvnw
COPY worker/.mvn ./.mvn

COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY worker/pom.xml ./worker/pom.xml
COPY backend/pom.xml ./backend/pom.xml

RUN chmod +x mvnw \
    && ./mvnw -f pom.xml -pl worker -am dependency:go-offline -B

COPY common/src ./common/src
COPY worker/src ./worker/src

RUN ./mvnw -f pom.xml -pl worker -am package -DskipTests -B

FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app
COPY --from=build /workspace/worker/target/worker-0.0.1-SNAPSHOT.jar ./app.jar

CMD ["java", "-jar", "/app/app.jar"]
