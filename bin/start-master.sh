master_dir=$(cd $(dirname $0);pwd)

source $master_dir/env.sh


java_cmd="$JAVA_HOME/bin/java -cp $MONITOR_CLASSPATH com.github.feilewu.monitor.core.deploy.master.Master"

echo "java command $java_cmd"

exec $java_cmd
