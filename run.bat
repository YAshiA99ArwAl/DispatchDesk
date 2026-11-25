@echo off
setlocal
cd /d "%~dp0"
title DispatchDesk - Modern SaaS Delivery Management System

echo ===================================================================
echo     DispatchDesk - Delivery & Fleet Management System
echo ===================================================================
echo.
echo Launching Java REST API Backend Server...
echo.
cd backend
call run-backend.bat
