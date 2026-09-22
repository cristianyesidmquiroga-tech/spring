#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
  # if there is no base dir set, use the current dir
  MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
fi

if [ -z "$MAVEN_OPTS" ]; then
  MAVEN_OPTS=""
fi

if [ -z "$MAVEN_DEBUG_OPTS" ]; then
  MAVEN_DEBUG_OPTS=""
fi

if [ -z "$JAVA_HOME" ]; then
  echo "Error: JAVA_HOME not found in your environment." >&2
  echo "Please set the JAVA_HOME variable in your environment to match the" >&2
  echo "location of your Java installation." >&2
  exit 1
fi

if [ -z "$MAVEN_CMD_LINE_ARGS" ]; then
  MAVEN_CMD_LINE_ARGS="$@"
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  
  if [ -n "$MVNW_VERBOSE" ]; then
    echo "Couldn't find $WRAPPER_JAR, downloading it ..."
    echo "Downloading from: $WRAPPER_URL"
  fi
  
  if command -v wget > /dev/null; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v curl > /dev/null; then
    curl -s -o "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: Cannot download maven-wrapper.jar. Install wget or curl." >&2
    exit 1
  fi
  
  if [ -n "$MVNW_VERBOSE" ]; then
    echo "Finished downloading $WRAPPER_JAR"
  fi
fi

exec "$JAVA_HOME/bin/java" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  $WRAPPER_LAUNCHER $MAVEN_CMD_LINE_ARGS
