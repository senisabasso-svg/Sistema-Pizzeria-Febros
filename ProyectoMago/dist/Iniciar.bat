@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist "ProyectoMago.jar" (
  echo No se encuentra ProyectoMago.jar en esta carpeta.
  pause
  exit /b 1
)

if not exist "lib\sqlite-jdbc-3.39.3.0.jar" (
  if exist "sqlite-jdbc-3.39.3.0.jar" (
    mkdir lib 2>nul
    copy /Y "sqlite-jdbc-3.39.3.0.jar" "lib\sqlite-jdbc-3.39.3.0.jar" >nul
  ) else (
    echo Falta lib\sqlite-jdbc-3.39.3.0.jar
    pause
    exit /b 1
  )
)

if not exist "prueba.db" (
  if exist "..\prueba.db" copy /Y "..\prueba.db" "prueba.db" >nul
)

echo Iniciando Sistema Pizzeria...
start "" javaw -jar "ProyectoMago.jar"
if errorlevel 1 (
  echo.
  echo Si falla, proba con: java -jar ProyectoMago.jar
  java -jar "ProyectoMago.jar"
  pause
)
