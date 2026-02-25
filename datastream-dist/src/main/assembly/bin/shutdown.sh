#Licensed to the Apache Software Foundation (ASF) under one or more
#contributor license agreements.  See the NOTICE file distributed with
#this work for additional information regarding copyright ownership.
#The ASF licenses this file to You under the Apache License, Version 2.0
#(the "License"); you may not use this file except in compliance with
#the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.

#!/usr/bin/env bash

#############################################
APP_MAIN="com.itman.datastream.starter.DataStreamApplication"
#############################################

APP_HOME="${BASH_SOURCE-$0}"
APP_HOME="$(dirname "${APP_HOME}")"
APP_HOME="$(cd "${APP_HOME}"; pwd)"
APP_HOME="$(cd "$(dirname ${APP_HOME})"; pwd)"
#echo "Base Directory:${APP_HOME}"

APP_BIN_PATH=$APP_HOME/bin
APP_LIB_PATH=$APP_HOME/lib
APP_CONF_PATH=$APP_HOME/conf

#把lib下的所有jar都加入到classpath中
CLASSPATH=""
for i in $APP_LIB_PATH/*.jar
do
	CLASSPATH="$i"
done

PIDS=`ps -ef | grep java | grep "$CLASSPATH" |awk '{print $2}'`
if [ -z "$PIDS" ]; then
        echo "ERROR: The server does not started!"
        exit 1
fi

echo -e "Stopping the server ...\c"
for PID in $PIDS ; do
        kill $PID > /dev/null 2>&1
done

COUNT=0
while [ $COUNT -lt 1 ]; do
        echo -e ".\c"
        sleep 1
        COUNT=1
        for PID in $PIDS ; do
                PID_EXIST=`ps -f -p $PID | grep java`
                if [ -n "$PID_EXIST" ]; then
                        COUNT=0
                        break
                fi
        done
done

echo "OK!"
echo "PID: $PIDS"
