FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && \
    ./gradlew bootJar -x test -x asciidoctor --no-daemon && \
    find build/libs -name "donggree-*.jar" ! -name "*-plain.jar" -exec mv {} build/libs/app.jar \;

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
