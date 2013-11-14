#! /usr/bin/env bash

pushd $(dirname $0) > /dev/null
SCRIPTPATH=$(pwd)
popd > /dev/null


PID_FILE="${SCRIPTPATH}/log/web.pid"

if [ -f "${PID_FILE}"  ] ; then
	PID=$(cat "${PID_FILE}")
	kill "${PID}"
	rm "${PID_FILE}"
fi
