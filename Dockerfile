# Build
FROM krmp-d2hub-idock.9rum.cc/goorm/gradle:8.8-jdk17

WORKDIR /app

COPY . .

RUN echo "systemProp.http.proxyHost=krmp-proxy.9rum.cc\nsystemProp.http.proxyPort=3128\nsystemProp.https.proxyHost=krmp-proxy.9rum.cc\nsystemProp.https.proxyPort=3128" > /root/.gradle/gradle.properties

RUN chmod +x ./gradlew
RUN ./gradlew clean build

# 노출할 포트 명시
EXPOSE 8080

# always do command
ENTRYPOINT ["java","-jar","/app/build/libs/balpyo-0.0.1-SNAPSHOT.jar"]
