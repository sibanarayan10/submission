FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

ARG GITHUB_TOKEN
ARG GITHUB_USERNAME

RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/target/*.jar app.jar
RUN chown app:app app.jar

EXPOSE 8082

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]