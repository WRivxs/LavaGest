# ====================================================
# SCRIPT DE LANZAMIENTO LAVAGEST PRO (POWERSHELL)
# ====================================================

if (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe") {
    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
} elseif (Test-Path "C:\Program Files\Java\jdk1.8.0_111\bin\javac.exe") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_111"
}

$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_OPTS = "-Dsun.java2d.d3d=true -Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true"

Write-Host "Iniciando LavaGest PRO con JDK en: $env:JAVA_HOME" -ForegroundColor Cyan
mvn clean compile exec:java
