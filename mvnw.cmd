@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM
@REM Downloads Maven if not present and runs it.
@REM Requires JAVA_HOME or java on PATH.
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"
set MAVEN_HOME="%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"
set MAVEN_CMD="%MAVEN_HOME%\bin\mvn.cmd"

@REM If Maven is already on PATH, use it
where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    mvn %*
    goto end
)

@REM Check if the wrapper jar exists; if not, download it
if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    for /f "tokens=2 delims==" %%a in ('findstr "wrapperUrl" %WRAPPER_PROPERTIES%') do set WRAPPER_URL=%%a
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile %WRAPPER_JAR%"
)

@REM Check if Maven distribution exists; if not, let the wrapper download it
if not exist %MAVEN_HOME% (
    echo Downloading Apache Maven 3.9.6...
    java -jar %WRAPPER_JAR% %*
    goto end
)

@REM Use the downloaded Maven
%MAVEN_CMD% %*

:end
endlocal
