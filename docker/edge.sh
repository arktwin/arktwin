#!/bin/bash -eu
if [ "$#" -eq 0 ]; then
  java -Dconfig.file=/etc/opt/arktwin/edge.conf -XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ZGenerational -jar /opt/arktwin/arktwin-edge.jar &
  PID=$!
  while [ ! -f /var/opt/arktwin/edge.shutdown ]; do
    sleep 1
  done
  kill -TERM $PID
  rm /var/opt/arktwin/edge.shutdown
else
  java -Dconfig.file=/etc/opt/arktwin/edge.conf -XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ZGenerational -jar /opt/arktwin/arktwin-edge.jar $@
fi
