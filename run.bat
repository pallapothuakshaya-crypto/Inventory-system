@echo off
title Inventory Management System - Spring Boot
echo ========================================================
echo   Inventory Management System (IMS) - Starting Server
echo ========================================================
echo.

set "MAVEN_USER_HOME=C:\Users\lahar\Downloads\Projects\.m2"
set "MAVEN_OPTS=-Dmaven.repo.local=C:\Users\lahar\Downloads\Projects\.m2\repository"

echo Starting Spring Boot Application on Port 8081...
echo Browser URL: http://localhost:8081
echo.
echo Default Accounts:
echo   - Admin: admin / admin123
echo   - User:  user  / user123
echo ========================================================
echo.

call mvnw.cmd spring-boot:run "-Dmaven.repo.local=C:\Users\lahar\Downloads\Projects\.m2\repository"
pause
