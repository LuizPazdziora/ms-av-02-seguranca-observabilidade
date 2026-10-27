# Etapa 1 - build e testes
FROM eclipse-temurin:17.0.20.1_1-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
ARG SKIP_TESTS=false
RUN ./mvnw -B clean package -DskipTests=${SKIP_TESTS}

# Etapa 2 - execucao
FROM eclipse-temurin:17.0.20.1_1-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app app && mkdir -p /data && chown app:app /data
WORKDIR /app
COPY --from=build /app/target/login-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080 8081
CMD ["java", "-jar", "app.jar"]
