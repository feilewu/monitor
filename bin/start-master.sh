master_dir=$(cd $(dirname $0);pwd)

source $master_dir/env.sh

java_options='-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005'

java_cmd="$JAVA_HOME/bin/java -cp $MONITOR_CLASSPATH $java_options com.github.feilewu.monitor.core.deploy.master.Master"

echo "java command $java_cmd"

exec $java_cmd
