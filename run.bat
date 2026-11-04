@echo off
setlocal
echo ====================================================
echo        INICIANDO LAVAGEST PRO - SWING UI
echo ====================================================

if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
) else if exist "C:\Program Files\Java\jdk1.8.0_111\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_111"
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_OPTS=-Dsun.java2d.d3d=true -Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true"

echo Usando JDK en: %JAVA_HOME%
echo.
mvn clean compile exec:java
pause
