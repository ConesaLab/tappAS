@echo off
setlocal enabledelayedexpansion

:: Configura JAVA_HOME si no está configurado (modifica la ruta según tu JDK)
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
)

set "APP_NAME=tappas"
set "MAIN_CLASS=tappas.Tappas"
set "JAR_NAME=tappas-jar-with-dependencies.jar"
set "PACKAGING_DIR=packaging"
set "APP_DIR=%PACKAGING_DIR%\app"
set "ICON_PATH=%PACKAGING_DIR%\icon.ico"
set "JAVA_MODULES=java.base,java.logging,java.desktop,java.sql,javafx.controls,javafx.fxml,javafx.web,javafx.swing"

:: Ir al directorio raíz del proyecto (uno arriba del script)
cd /d "%~dp0\.."

echo === 1. Building fat JAR with Maven...
call mvn clean package
if errorlevel 1 (
    echo [ERROR] Falló la compilación con Maven.
    exit /b 1
)

echo === 2. Creating runtime...
if exist "%PACKAGING_DIR%\runtime" rmdir /s /q "%PACKAGING_DIR%\runtime"

call "%JAVA_HOME%\bin\jlink.exe" --module-path "%JAVA_HOME%\jmods;%PACKAGING_DIR%\javafx\javafx-jmods-24-win" --add-modules %JAVA_MODULES% --output "%PACKAGING_DIR%\runtime_win" --compress=2 --no-header-files --no-man-pages
if errorlevel 1 (
    echo [ERROR] Falló la creación del runtime con jlink.
    exit /b 1
)

echo === 3. Copiying JAR to jpackage directory...
if not exist "%APP_DIR%" mkdir "%APP_DIR%"
copy "target\%JAR_NAME%" "%APP_DIR%\" > nul
if errorlevel 1 (
    echo [ERROR] Falló la copia del JAR.
    exit /b 1
)

echo === 4. Creating package...
call "%JAVA_HOME%\bin\jpackage.exe" --type app-image --name "%APP_NAME%" --input "%APP_DIR%" --main-jar "%JAR_NAME%" --main-class "%MAIN_CLASS%" --icon "%ICON_PATH%" --runtime-image "%PACKAGING_DIR%\runtime_win" --dest dist/win --java-options "--enable-preview --add-exports=javafx.controls/com.sun.javafx.charts=ALL-UNNAMED --add-exports=javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED -Xms512m -XX:MaxRAMPercentage=75"
if errorlevel 1 (
    echo [ERROR] Falló jpackage.
    exit /b 1
)

echo === ✅ Package created correctly in .\dist\win\%APP_NAME%

endlocal
pause
