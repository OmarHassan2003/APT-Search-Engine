#!/usr/bin/env bash
set -euo pipefail

########################
# Configuration
########################

# Path to your dotenv files
APP_ENV_FILE=".env"
MONGO_ENV_FILE="docker.env"

# Docker volume & container names
VOLUME="mongodb_data"
CONTAINER="mongodb"

# Mongo port mapping
HOST_PORT=27017
CONTAINER_PORT=27017

# How long to wait for Mongo to start (seconds)
WAIT_TIME=10

########################
# Helper functions
########################

function load_app_env() {
  if [[ ! -f $APP_ENV_FILE ]]; then
    echo "❌  Cannot find $APP_ENV_FILE"
    exit 1
  fi
  # Export non-comment lines
  export $(grep -v '^\s*#' "$APP_ENV_FILE" | xargs)
}

function start_mongo() {
  echo "🔄  Cleaning up any old MongoDB container..."
  docker rm -f "$CONTAINER" 2>/dev/null || true

  echo "📦  Ensuring Docker volume exists: $VOLUME"
  docker volume create "$VOLUME" 2>/dev/null || true

  echo "🐳  Launching MongoDB container..."
  docker run -d \
    --name "$CONTAINER" \
    -p "$HOST_PORT:$CONTAINER_PORT" \
    -v "$VOLUME":/data/db \
    --env-file "$MONGO_ENV_FILE" \
    mongo:latest >/dev/null

  echo "⏳  Waiting $WAIT_TIME seconds for MongoDB to initialize..."
  sleep "$WAIT_TIME"
  echo "✅  MongoDB should be up!"
}

function build_and_run_app() {
  echo "🛠️  Building project with Maven..."
  mvn clean package -q

  echo "🚀  Locating JAR to run..."
  JAR=$(find target -maxdepth 1 -name '*.jar' | head -n1)
  if [[ -z "$JAR" ]]; then
    echo "❌  No JAR found in target/—did the build succeed?"
    exit 1
  fi

  echo "🏃  Running application..."
  java -jar "$JAR"
}

########################
# Main
########################

echo "==============================="
echo "⚙️  Starting APT‑SEARCH‑ENGINE"
echo "==============================="

load_app_env
start_mongo
build_and_run_app
