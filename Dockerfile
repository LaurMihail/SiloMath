FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY backend backend
COPY calculation-engine calculation-engine

RUN chmod +x gradlew \
    && ./gradlew -p backend --no-daemon clean installDist

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

COPY --from=builder /workspace/backend/build/install/silomath-backend/ /app/

ENV SILOMATH_BACKEND_HOST=0.0.0.0
ENV SILOMATH_BACKEND_PORT=10000

EXPOSE 10000

ENTRYPOINT ["/app/bin/silomath-backend"]
