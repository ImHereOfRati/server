FROM amazoncorretto:25-alpine-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY del src
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM amazoncorretto:25-alpine-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
