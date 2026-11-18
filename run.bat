@echo off
title DispatchDesk
cd /d "%~dp0"
echo Starting DispatchDesk...
if exist "C:\Users\palak\.jdks\ms-21.0.13.0.2\bin\java.exe" (
    set "JAVA_EXE=C:\Users\palak\.jdks\ms-21.0.13.0.2\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)
"%JAVA_EXE%" -cp "target\classes;C:\Users\palak\.m2\repository\org\xerial\sqlite-jdbc\3.50.3.0\sqlite-jdbc-3.50.3.0.jar" com.dispatchdesk.Main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo An error occurred while launching DispatchDesk.
    pause
)
