#!/bin/bash
CLASS_TO_RUN="de.tuberlin.cit.livescale.dispatcher.Dispatcher"

pushd $(dirname $0) > /dev/null
SCRIPTPATH=$(pwd)
popd > /dev/null

#---------------------------------#
# dynamically build the classpath #
#---------------------------------#
THE_CLASSPATH=""
for i in $(ls "${SCRIPTPATH}"/lib/*.jar)
do
  THE_CLASSPATH="${THE_CLASSPATH}:${i}"
done

set -x
java -cp "${THE_CLASSPATH}" "${CLASS_TO_RUN}"  > "${SCRIPTPATH}/log/dispatcher.out" 2>&1 < /dev/null &
echo $! > "${SCRIPTPATH}/log/dispatcher.pid"
