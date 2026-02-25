@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.  See the NOTICE file distributed with
@REM this work for additional information regarding copyright ownership.
@REM The ASF licenses this file to You under the Apache License, Version 2.0
@REM (the "License"); you may not use this file except in compliance with
@REM the License.  You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.

@echo off
title datastream
setlocal enabledelayedexpansion
cls

set APP_MAINCLASS=com.itman.datastream.starter.DataStreamApplication

set APP_HOME=%~dp0
set APP_HOME=%APP_HOME%\..\
cd %APP_HOME%
set APP_HOME=%cd%

set APP_BIN_PATH=%APP_HOME%\bin
set APP_LIB_PATH=%APP_HOME%\lib
set APP_CONF_PATH=%APP_HOME%\conf
set APP_FILE=-Xbootclasspath/p:%APP_CONF_PATH%

set JAVA_OPTS=-server -Xms4096m -Xmx4096m -Xmn2048m -XX:+DisableExplicitGC -Djava.awt.headless=true -Dfile.encoding=UTF-8

@echo off
set CLASSPATH=
for %%i in (%APP_LIB_PATH%\*.jar) do (
    set CLASSPATH=%%i
)

echo System Information:
echo ********************************************************
echo COMPUTERNAME=%COMPUTERNAME%
echo OS=%OS%
echo.
echo APP_HOME=%APP_HOME%
echo APP_MAINCLASS=%APP_MAINCLASS%
echo CLASSPATH: %CLASSPATH%
echo CURRENT_DATE=%date% %time%:~0,8%
echo ********************************************************

echo Starting %APP_MAINCLASS% ...
echo java %APP_FILE% -jar %CLASSPATH%\* %JAVA_OPTS%
echo .
java %APP_FILE% -jar %CLASSPATH% %JAVA_OPTS%

:exit
pause