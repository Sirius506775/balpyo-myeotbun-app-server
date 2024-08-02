# Build
FROM krmp-d2hub-idock.9rum.cc/goorm/gradle:8.8-jdk17 AS builder

WORKDIR /app

COPY . .

RUN echo "systemProp.http.proxyHost=krmp-proxy.9rum.cc\nsystemProp.http.proxyPort=3128\nsystemProp.https.proxyHost=krmp-proxy.9rum.cc\nsystemProp.https.proxyPort=3128" > /root/.gradle/gradle.properties

RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# App image
FROM krmp-d2hub-idock.9rum.cc/goorm/openjdk:17-jdk-alpine
LABEL authors="이동헌"

# 앱 디렉토리 생성
WORKDIR /app

# 컨테이너의 파일시스템에 빌드된 jar파일 복사
COPY --from=builder /app/build/libs .

# docker image timezone 설정
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 앱 실행을 위한 사용자 계정 생성
RUN addgroup --system dockeruser && adduser --system --ingroup dockeruser dockeruser

# /app 폴더 소유권을 신규 사용자 계정으로 변경
RUN chown -R dockeruser:dockeruser /app

# 사용자 계정으로 전환
USER dockeruser

# 노출할 포트 명시
EXPOSE 8080

# always do command
ENTRYPOINT ["java","-jar","/app/balpyo-0.0.1-SNAPSHOT.jar"]
