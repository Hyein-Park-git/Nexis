@echo off
set SERVICE_NAME=NexisAgent
set INSTALL_DIR=C:\NexisAgent

REM Java 자동 탐색
for /f "delims=" %%i in ('where java 2^>nul') do (
    set JAVA_EXE=%%i
    goto :foundJava
)

:foundJava
if "%JAVA_EXE%"=="" exit /b 1

"%INSTALL_DIR%\nssm.exe" install %SERVICE_NAME% "%JAVA_EXE%" -jar "%INSTALL_DIR%\agent-1.0.jar"

sc config %SERVICE_NAME% start= demand

exit /b 0