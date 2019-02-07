#!/usr/bin/env bash

set -eo pipefail

while [ $# -gt 0 ]; do
  case "$1" in
    --path-to-configuration-properties-file=*) # required
      CP_FILE="${1#*=}"
      ;;
    --context-path=*)                          # required
      CONTEXT_PATH="${1#*=}"
      ;;
    *)
      printf "Error: Invalid argument ${1}\n"
      exit 1
  esac
  shift
done

if [ -z "$CP_FILE" ]; then
  printf "Error: --path-to-configuration-properties-file in required\n"
  exit 1
fi
if [ -z "$CONTEXT_PATH" ]; then
  printf "Error: --context-path in required\n"
  exit 1
fi

# "configure" application using passed configuration file
echo $CP_FILE
cat $CP_FILE
echo "ln -s $CP_FILE $APPLICATION_PATH/WEB-INF/conf/configuration.properties"
# it is not clear why linked file does not work !?!
# ln -s $CP_FILE $APPLICATION_PATH/WEB-INF/conf/configuration.properties
cp $CP_FILE $APPLICATION_PATH/WEB-INF/conf/configuration.properties

# "install" application into tomcat using specified context path
LINKED_APPLIATION_PATH=$TOMCAT_HOME/webapps$(echo $CONTEXT_PATH | sed -e 's/\//#/g' -e 's/#/\//1')
echo "ln -s $APPLICATION_PATH $LINKED_APPLIATION_PATH"
ln -s $APPLICATION_PATH $LINKED_APPLIATION_PATH

# setup java options
export JAVA_OPTS="-verbose:gc -XX:+PrintGCDetails -Xloggc:$TOMCAT_HOME/logs/gc.log -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime -XX:+HeapDumpOnOutOfMemoryError -Xms512m -Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:ReservedCodeCacheSize=256m -XX:+UseCodeCacheFlushing -Dfile.encoding=UTF-8 -XX:+UseG1GC"

# Run Tomcat
$TOMCAT_HOME/bin/catalina.sh run
