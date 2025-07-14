#!/bin/bash
set -e

APP_NAME="tappas"
MAIN_CLASS="tappas.Tappas"
JAR_NAME="tappas-jar-with-dependencies.jar"
PACKAGING_DIR="packaging"
APP_DIR="$PACKAGING_DIR/app"
ICON_PATH="$PACKAGING_DIR/icon.icns" # .icns para macOS
JAVA_MODULES="java.base,java.logging,java.desktop,java.sql,javafx.controls,javafx.fxml,javafx.web,javafx.swing"

cd "$(dirname "$0")/.."

echo "Building fat jar with Maven..."
mvn clean package

echo "Creating custom runtime image with jlink..."
rm -rf "$PACKAGING_DIR/runtime"
jlink \
  --module-path "$JAVA_HOME/jmods:$PACKAGING_DIR/javafx/javafx-jmods-24-macos" \
  --add-modules $JAVA_MODULES \
  --output "$PACKAGING_DIR/runtime_macos" \
  --compress=2 \
  --no-header-files \
  --no-man-pages

echo "Copying jar to app directory..."
mkdir -p "$APP_DIR"
cp "target/$JAR_NAME" "$APP_DIR/"

echo "Packaging app with jpackage..."
jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --input "$APP_DIR" \
  --main-jar "$JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  --icon "$ICON_PATH" \
  --runtime-image "$PACKAGING_DIR/runtime_macos" \
  --dest dist/macos \
  --java-options "--enable-preview \
    --add-exports=javafx.controls/com.sun.javafx.charts=ALL-UNNAMED \
    --add-exports=javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED \
    --add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED \
    -Xms8g -XX:MaxRAMPercentage=75"

echo "Package created in ./dist/$APP_NAME"
