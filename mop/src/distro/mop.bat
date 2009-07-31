@echo off

REM ------------------------------------------------------------------------

if exist "%HOME%\moprc_pre.bat" call "%HOME%\moprc_pre.bat"

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_MVNRUN_HOME=%~dp0

if "%MVNRUN_HOME%"=="" set MVNRUN_HOME=%DEFAULT_MVNRUN_HOME%
set DEFAULT_MVNRUN_HOME=

:doneStart
rem find MVNRUN_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%MVNRUN_HOME%\README.txt" goto checkJava

rem check for mop in Program Files on system drive
if not exist "%SystemDrive%\Program Files\mop" goto checkSystemDrive
set MVNRUN_HOME=%SystemDrive%\Program Files\mop
goto checkJava

:checkSystemDrive
rem check for mop in root directory of system drive
if not exist %SystemDrive%\mop\README.txt goto checkCDrive
set MVNRUN_HOME=%SystemDrive%\mop
goto checkJava

:checkCDrive
rem check for mop in C:\mop for Win9X users
if not exist C:\mop\README.txt goto noAntHome
set MVNRUN_HOME=C:\mop
goto checkJava

:noAntHome
echo MVNRUN_HOME is set incorrectly or mop could not be located. Please set MVNRUN_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto runAnt

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo.

:runAnt

if "%MVNRUN_BASE%" == "" set MVNRUN_BASE=%MVNRUN_HOME%

if "%MVNRUN_OPTS%" == "" set MVNRUN_OPTS=-Xmx512M -Dorg.apache.mop.UseDedicatedTaskRunner=true

if "%SUNJMX%" == "" set SUNJMX=-Dcom.sun.management.jmxremote
REM set SUNJMX=-Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

REM Uncomment to enable YourKit profiling
REM SET MVNRUN_DEBUG_OPTS="-agentlib:yjpagent"

REM Uncomment to enable remote debugging
REM SET MVNRUN_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

REM Setup MVNRUN Classpath. Default is the conf directory.
set MVNRUN_CLASSPATH=%MVNRUN_BASE%/conf;%MVNRUN_CLASSPATH%

"%_JAVACMD%" %SUNJMX% %MVNRUN_DEBUG_OPTS% %MVNRUN_OPTS% %SSL_OPTS% -Dmop.classpath="%MVNRUN_CLASSPATH%" -Dmop.home="%MVNRUN_HOME%" -Dmop.base="%MVNRUN_BASE%" -jar "%MVNRUN_HOME%/mop.jar" %*

goto end


:end
set _JAVACMD=
if "%OS%"=="Windows_NT" @endlocal

:mainEnd
if exist "%HOME%\moprc_post.bat" call "%HOME%\moprc_post.bat"


