FROM maven:3.9.7-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package


FROM amazoncorretto:21-alpine

ARG JAR_FILE=/workspace/target/*jar
COPY --from=builder ${JAR_FILE} app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:+TieredCompilation -XX:TieredStopAtLevel=1 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} -jar /app.jar"]
