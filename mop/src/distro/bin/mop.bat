@REM
@REM  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
@REM  http://fusesource.com
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM
@echo off

REM ------------------------------------------------------------------------
if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_MOP_HOME=%~dp0..

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
if not exist C:\mop\README.txt goto noMopHome
set MOP_HOME=C:\mop
goto checkJava

:noMopHome
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

if "%MOP_OPTS%" == "" set MOP_OPTS=-Xmx512M 

if "%SUNJMX%" == "" set SUNJMX=-Dcom.sun.management.jmxremote
REM set SUNJMX=-Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

REM Uncomment to enable YourKit profiling
REM SET MOP_DEBUG_OPTS="-agentlib:yjpagent"

REM Uncomment to enable remote debugging
REM SET MOP_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

"%_JAVACMD%" %MOP_DEBUG_OPTS% %SUNJMX% %MOP_OPTS% -Dmop.classpath="%MOP_CLASSPATH%" -Dmop.home="%MOP_HOME%" -Dmop.base="%MOP_BASE%" -jar "%MOP_HOME%/bin/mop.jar" %*

goto end

:end
set _JAVACMD=
if "%OS%"=="Windows_NT" @endlocal

