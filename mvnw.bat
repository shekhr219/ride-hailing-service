@echo off
set "MAVEN_EXE=D:\Games\IntelliJ IDEA Community Edition 2025.1.7\plugins\maven\lib\maven3\bin\mvn.cmd"
if exist "%MAVEN_EXE%" (
    call "%MAVEN_EXE%" %*
) else (
    call mvn %*
)
