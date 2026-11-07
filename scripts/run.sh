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
: "${SERVICE_HOST:?Set SERVICE_HOST}"
: "${SERVICE_PASSWORD:?Set SERVICE_PASSWORD}"
export JAVA_HOME SERVICE_HOST SERVICE_PASSWORD
./scripts/mvn.sh -B -pl lambda-example-app -am package
exec "$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar lambda-example-app/target/lambda-example-app-1.0.0-SNAPSHOT.jar "$@"
