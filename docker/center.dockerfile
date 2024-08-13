FROM eclipse-temurin:21 AS jre-build
RUN $JAVA_HOME/bin/jlink \
        --add-modules java.se,jdk.httpserver,jdk.jcmd,jdk.unsupported \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-9 \
        --output /javaruntime

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.2_13_1.10.1_3.3.3 AS jar-build
WORKDIR /arktwin/
COPY arktwin/ /arktwin/
RUN sbt assembly

FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN apt-get update && \
    apt-get install -y curl jq && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN mkdir /opt/arktwin/ && \
    mkdir /etc/opt/arktwin/ && \
    touch /etc/opt/arktwin/center.conf
COPY --from=jre-build /javaruntime $JAVA_HOME
COPY --from=jar-build /arktwin/center/target/scala-3.5.0/arktwin-center.jar /opt/arktwin/arktwin-center.jar
ENTRYPOINT ["java", "-Dconfig.file=/etc/opt/arktwin/center.conf", "-XX:MaxRAMPercentage=75", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "/opt/arktwin/arktwin-center.jar"]
