FROM gradle:8.0-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle /app/
# Copy the gradle wrapper so it exists
COPY gradle /app/gradle
COPY gradlew /app/
# Copy all source files
COPY . /app/
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# We don't know the exact jar name, so we'll just copy whatever is built
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
