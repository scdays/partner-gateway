#!/bin/bash

start() {
	# Check if it is already running
	echo $"--- The {0} is starting...`date` "
      pid=`ps -ef | grep java | grep {0}.jar | awk '{print $2}'`
	if [ ! -z "${pid}" ]; then
	    echo "--- {0} has started already"
	    return 1
	fi
	if [ ! -f META-INF/MANIFEST.MF ]; then
            jar -xf lib/{0}.jar META-INF/MANIFEST.MF
            sed -i 's/\r//' META-INF/MANIFEST.MF
    fi
    lines=`cat META-INF/MANIFEST.MF`
    jvmstart=0
    jvmend=0
    for line in $lines;do
      if [ "$line" = "JVM:" ] && [ $jvmstart -eq 0 ];then
        jvmstart=1
        continue
      fi
      if [ "$line" = "Spring-Boot-Classpath-Index:" ] && [ $jvmend -eq 0 ];then
        jvmend=1
      fi
      if [ $jvmstart -eq 1 ] && [ $jvmend -ne 1 ];then
        if [[ "$line" =~ -X.* ]];then
            jvm=$jvm" "$line
        else
            jvm=$jvm$line
        fi
      fi
    done
  nohup java -jar $jvm $* lib/{0}.jar>/dev/null 2>&1 &
  sleep 5
  log
}

stop() {
	echo -n $"--- The {0} is stoping... "
	pid=`ps -ef | grep java | grep {0}.jar |awk '{print $2}'`
    if [ ! -z "${pid}" ]; then
		kill -9 $pid
		echo " [ OK ]"
		return 0
	else
		echo " [ FAILED ], {0} has stopped already"
		return 1
	fi
}

status() {
	pid=`ps -ef | grep java | grep {0}.jar |awk '{print $2}'`
	if [ ! -z "${pid}" ]; then
		echo "--- {0} is runnig, pid is ${pid}"
	else
		echo "--- {0} is stopped"
	fi
	return 0
}

restart() {
	stop
	start $*
}

log() {
   status
   if [ ! -z "${pid}" ]; then
   	log=`lsof -p $pid | grep "logs/.*[0-9]*\.log"| awk '{print $NF}'`
   	tail -f $log
   fi
}

case "$1" in
start)
	$*
	;;
stop)
	stop
	;;
restart)
	$*
	;;
status)
	status
	;;
log)
	log
	;;
*)
	echo $"Usage: $0 {start|stop|status|restart|log}"
	exit 1
esac

exit 0