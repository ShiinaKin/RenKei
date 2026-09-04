FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon buildFatJar

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /build/build/libs/backend-all.jar /app/renkei.jar

ENV DATABASE_DRIVER="org.sqlite.JDBC"
ENV DATABASE_URL="jdbc:sqlite:data/renkei.db"
ENV DATABASE_USERNAME=""
ENV DATABASE_PASSWORD=""

ENV JAVA_OPTS="-Xmx256M"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/renkei.jar"]
