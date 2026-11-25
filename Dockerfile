# ---- build ----
FROM eclipse-temurin:27-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package && mv target/budget-tracker-server-*.jar app.jar

# ---- run ----
FROM eclipse-temurin:27-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /app/app.jar app.jar
USER appuser
# The port comes from the PORT env var (default 8080). Health check path: /actuator/health
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
