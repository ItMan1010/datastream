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
APP_PID_FILE="${APP_HOME}/run/${APP_MAIN}.pid"
APP_RUN_LOG="${APP_HOME}/run/run_${APP_MAIN}.log"

[ -d "${APP_HOME}/run" ] || mkdir -p "${APP_HOME}/run"
cd ${APP_HOME}

echo -n `date +'%Y-%m-%d %H:%M:%S'`              >>${APP_RUN_LOG}
echo "---- Start service [${APP_MAIN}] process. ">>${APP_RUN_LOG}

# JVMFLAGS JVM参数可以在这里设置
JVMFLAGS="-Dfile.encoding=UTF-8 -server -Xms4096m -Xmx4096m -Xmn2048m -XX:+DisableExplicitGC "

if [ "$JAVA_HOME" != "" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

#把lib下的所有jar都加入到classpath中
CLASSPATH=""
for i in $APP_LIB_PATH/*.jar
do
	CLASSPATH="$i"
done

APP_FILE="-Xbootclasspath/p:$APP_CONF_PATH/"

res=`ps aux|grep java|grep $CLASSPATH|grep -v grep|awk '{print $2}'`
if [ -n "$res"  ]; then
        echo "$res program is already running"
        exit 1
fi

nohup $JAVA $APP_FILE -jar $CLASSPATH $JVMFLAGS >>${APP_RUN_LOG} 2>&1 &

RETVAL=$?
PID=$!

echo ${PID} >${APP_PID_FILE}
exit ${RETVAL}

