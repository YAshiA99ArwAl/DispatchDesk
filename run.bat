@echo off
title DispatchDesk
cd /d "%~dp0"
echo Starting DispatchDesk...
if exist "%USERPROFILE%\.jdks\ms-21.0.13.0.2\bin\java.exe" (
    set "JAVA_EXE=%USERPROFILE%\.jdks\ms-21.0.13.0.2\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)
set "SQLITE_JAR=%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.50.3.0\sqlite-jdbc-3.50.3.0.jar"
"%JAVA_EXE%" -cp "frontend\target\classes;backend\target\classes;database\target\classes;%SQLITE_JAR%" com.dispatchdesk.Main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo An error occurred while launching DispatchDesk.
    pause
)
