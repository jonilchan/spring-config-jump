@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "JAVA_HOME=%USERPROFILE%\.jdks\corretto-17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    for /d %%d in ("%USERPROFILE%\.jdks\*17*") do set "JAVA_HOME=%%d"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    for /d %%d in ("C:\Program Files\Java\*17*") do set "JAVA_HOME=%%d"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    for /d %%d in ("C:\Program Files\Amazon Corretto\*17*") do set "JAVA_HOME=%%d"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 17 not found. Please set JAVA_HOME manually.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ==============================
echo  Spring Config Jump - Build
echo ==============================
echo JAVA_HOME: %JAVA_HOME%
java -version 2>&1 | findstr /i "version"
echo.

if "%~1"=="" goto build
if "%~1"=="build" goto build
if "%~1"=="clean" goto clean
if "%~1"=="run" goto run
if "%~1"=="all" goto all
goto usage

:clean
echo [*] Cleaning...
call gradlew.bat clean
if errorlevel 1 exit /b 1
echo [OK] Clean complete.
goto end

:build
echo [*] Building plugin...
call gradlew.bat buildPlugin
if errorlevel 1 exit /b 1
echo.
echo [OK] Build complete. Plugin zip:
dir /b build\distributions\*.zip 2>nul
goto end

:run
echo [*] Launching IDE with plugin...
call gradlew.bat runIde
goto end

:all
echo [*] Clean + Build...
call gradlew.bat clean buildPlugin
if errorlevel 1 exit /b 1
echo.
echo [OK] All done. Plugin zip:
dir /b build\distributions\*.zip 2>nul
goto end

:usage
echo Usage: %~nx0 {clean^|build^|run^|all}
echo.
echo   clean   - Clean build output
echo   build   - Build plugin zip (default)
echo   run     - Launch a sandbox IDEA with the plugin
echo   all     - Clean + Build
exit /b 1

:end
endlocal
