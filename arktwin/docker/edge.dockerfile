FROM eclipse-temurin:21 AS jre-build
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.se,jdk.httpserver,jdk.jcmd,jdk.unsupported \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=zip-9 \
         --output /javaruntime

FROM debian:bullseye-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && apt-get clean && rm -rf /var/lib/apt/lists/*
RUN mkdir /opt/arktwin/ && \
    mkdir /etc/opt/arktwin/ && \
    touch /etc/opt/arktwin/edge.conf
COPY edge/target/scala-3.3.3/arktwin-edge.jar /opt/arktwin/arktwin-edge.jar
COPY docker/edge.sh /opt/arktwin/entrypoint.sh
ENTRYPOINT ["/opt/arktwin/entrypoint.sh"]
