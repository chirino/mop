@echo off

REM ------------------------------------------------------------------------

if exist "%HOME%\moprc_pre.bat" call "%HOME%\moprc_pre.bat"

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_MOP_HOME=%~dp0

if "%MOP_HOME%"=="" set MOP_HOME=%DEFAULT_MOP_HOME%
set DEFAULT_MOP_HOME=

:doneStart
rem find MOP_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%MOP_HOME%\README.txt" goto checkJava

rem check for mop in Program Files on system drive
if not exist "%SystemDrive%\Program Files\mop" goto checkSystemDrive
set MOP_HOME=%SystemDrive%\Program Files\mop
goto checkJava

:checkSystemDrive
rem check for mop in root directory of system drive
if not exist %SystemDrive%\mop\README.txt goto checkCDrive
set MOP_HOME=%SystemDrive%\mop
goto checkJava

:checkCDrive
rem check for mop in C:\mop for Win9X users
if not exist C:\mop\README.txt goto noAntHome
set MOP_HOME=C:\mop
goto checkJava

:noAntHome
echo MOP_HOME is set incorrectly or mop could not be located. Please set MOP_HOME.
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

if "%MOP_BASE%" == "" set MOP_BASE=%MOP_HOME%

if "%MOP_OPTS%" == "" set MOP_OPTS=-Xmx512M -Dorg.apache.mop.UseDedicatedTaskRunner=true

if "%SUNJMX%" == "" set SUNJMX=-Dcom.sun.management.jmxremote
REM set SUNJMX=-Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

REM Uncomment to enable YourKit profiling
REM SET MOP_DEBUG_OPTS="-agentlib:yjpagent"

REM Uncomment to enable remote debugging
REM SET MOP_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

REM Setup MOP Classpath. Default is the conf directory.
set MOP_CLASSPATH=%MOP_BASE%/conf;%MOP_CLASSPATH%

"%_JAVACMD%" %SUNJMX% %MOP_DEBUG_OPTS% %MOP_OPTS% %SSL_OPTS% -Dmop.classpath="%MOP_CLASSPATH%" -Dmop.home="%MOP_HOME%" -Dmop.base="%MOP_BASE%" -jar "%MOP_HOME%/mop.jar" %*

goto end


:end
set _JAVACMD=
if "%OS%"=="Windows_NT" @endlocal

:mainEnd
if exist "%HOME%\moprc_post.bat" call "%HOME%\moprc_post.bat"


