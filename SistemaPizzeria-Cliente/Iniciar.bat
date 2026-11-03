@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================
echo   Sistema Pizzeria - Inicio local
echo ========================================
echo.

where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: No se encontro Java instalado.
  echo Instala Java 17 o superior y volve a intentar.
  pause
  exit /b 1
)

if not exist "ProyectoMago.jar" (
  echo ERROR: Falta ProyectoMago.jar
  pause
  exit /b 1
)

if not exist "lib\sqlite-jdbc-3.39.3.0.jar" (
  echo ERROR: Falta lib\sqlite-jdbc-3.39.3.0.jar
  pause
  exit /b 1
)

if not exist "prueba.db" (
  echo No hay base de datos. Se creara al iniciar...
)

echo Abriendo sistema...
start "" javaw -jar "ProyectoMago.jar"
if errorlevel 1 (
  echo.
  echo Fallo javaw, probando con java...
  java -jar "ProyectoMago.jar"
  pause
)
