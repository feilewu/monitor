agent_dir=$(cd $(dirname $0);pwd)

source $agent_dir/env.sh

java_cmd="$JAVA_HOME/bin/java -cp $MONITOR_CLASSPATH com.github.feilewu.monitor.core.deploy.agent.Agent"

echo "java command $java_cmd"

exec $java_cmd

