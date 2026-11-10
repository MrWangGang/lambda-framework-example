#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi
: "${JAVA_HOME:?Set JAVA_HOME to JDK 27}"
: "${NACOS_SERVER_ADDR:?Set NACOS_SERVER_ADDR}"
export JAVA_HOME NACOS_SERVER_ADDR
./scripts/mvn.sh -B -pl service-b -am package
exec "$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar service-b/target/lambda-example-service-b-1.0.0-SNAPSHOT.jar "$@"
