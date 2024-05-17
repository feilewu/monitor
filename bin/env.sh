#!/bin/bash
set -e
set -o pipefail

script_dir=$(cd $(dirname $0);pwd)

if [ ! -n "$MONITOR_HOME" ]; then
 export MONITOR_HOME=$(dirname $script_dir)
fi

if [ ! -n "$JAVA_HOME" ]; then
 export JAVA_HOME=
fi

export MONITOR_CLASSPATH=$MONITOR_HOME/lib/*:


