ARG JAVA_RUNTIME_IMAGE
FROM ${JAVA_RUNTIME_IMAGE}

WORKDIR /app
COPY app.jar /app/app.jar
ENV HOME=/tmp
USER 10001:10001
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-Duser.home=/tmp", "-jar", "/app/app.jar"]
