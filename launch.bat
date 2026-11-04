@echo off
title LavaGest PRO
echo ====================================
echo         LavaGest PRO
echo ====================================
echo.

REM Verificar que Java este instalado
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no encontrado. Instala Java desde:
    echo https://adoptium.net
    pause
    exit /b 1
)

REM Verificar que el archivo .env exista
if not exist ".env" (
    echo ERROR: No se encontro el archivo .env
    echo Crea un archivo .env con las credenciales de la base de datos.
    echo Ejemplo en .env.example
    pause
    exit /b 1
)

echo Iniciando LavaGest PRO...
java -Dsun.java2d.d3d=true -Dsun.java2d.noddraw=true -jar LavaGest.jar
pause
