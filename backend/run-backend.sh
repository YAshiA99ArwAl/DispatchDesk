#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SQLITE_JAR="lib/sqlite-jdbc-3.50.3.0.jar"
if [ ! -f "$SQLITE_JAR" ] && [ -f "${HOME}/.m2/repository/org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar" ]; then
  SQLITE_JAR="${HOME}/.m2/repository/org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar"
fi

mkdir -p target/classes
echo "Compiling Java backend sources..."
javac --release 21 -encoding UTF-8 -cp "$SQLITE_JAR" -d target/classes \
  src/main/java/com/dispatchdesk/model/*.java \
  src/main/java/com/dispatchdesk/config/*.java \
  src/main/java/com/dispatchdesk/service/*.java \
  src/main/java/com/dispatchdesk/api/*.java \
  src/main/java/com/dispatchdesk/BackendMain.java

echo "Starting Backend API on http://localhost:8080 ..."
exec java -cp "target/classes:${SQLITE_JAR}" com.dispatchdesk.BackendMain
