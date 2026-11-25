@echo off
setlocal enabledelayedexpansion
title DispatchDesk Java Backend Server
cd /d "%~dp0"

echo ============================================
echo   DispatchDesk Java Backend Server
echo ============================================

set "JAVA_EXE="
set "JAVAC_EXE="

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
    )
)

if not defined JAVA_EXE (
    if exist "%USERPROFILE%\.jdks\temurin-21.0.13\bin\java.exe" (
        set "JAVA_EXE=%USERPROFILE%\.jdks\temurin-21.0.13\bin\java.exe"
        set "JAVAC_EXE=%USERPROFILE%\.jdks\temurin-21.0.13\bin\javac.exe"
    )
)

if not defined JAVA_EXE (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_EXE=%%D\bin\java.exe"
            set "JAVAC_EXE=%%D\bin\javac.exe"
        )
    )
)

if not defined JAVA_EXE (
    set "JAVA_EXE=java"
    set "JAVAC_EXE=javac"
)

set "SQLITE_JAR=lib\sqlite-jdbc-3.50.3.0.jar"
if not exist "%SQLITE_JAR%" (
    if exist "%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.50.3.0\sqlite-jdbc-3.50.3.0.jar" (
        set "SQLITE_JAR=%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.50.3.0\sqlite-jdbc-3.50.3.0.jar"
    ) else (
        echo [ERROR] Could not find sqlite-jdbc-3.50.3.0.jar in backend\lib or .m2 repository.
        echo Please ensure backend\lib\sqlite-jdbc-3.50.3.0.jar exists or run "mvn compile".
        pause
        exit /b 1
    )
)

if not exist "target\classes" mkdir "target\classes"
echo Compiling Java backend sources...
"%JAVAC_EXE%" --release 21 -encoding UTF-8 -cp "%SQLITE_JAR%" -d target\classes ^
    src\main\java\com\dispatchdesk\model\*.java ^
    src\main\java\com\dispatchdesk\config\*.java ^
    src\main\java\com\dispatchdesk\service\*.java ^
    src\main\java\com\dispatchdesk\api\*.java ^
    src\main\java\com\dispatchdesk\BackendMain.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)

echo.
echo ========================================================
echo   Backend Server starting on http://localhost:8080
echo   Health check: http://localhost:8080/api/health
echo   Web UI      : http://localhost:8080/
echo ========================================================
echo.
"%JAVA_EXE%" -cp "target\classes;%SQLITE_JAR%" com.dispatchdesk.BackendMain
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Server exited with error code %ERRORLEVEL%.
    pause
)
