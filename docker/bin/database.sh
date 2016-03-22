#!/bin/bash

VERSION=""
PROJECT=`basename $(realpath $(dirname $0)/../..)`
PROJECT_PATH=$(realpath $(dirname $0)/../..)
NAME=`basename $0 | awk '{ gsub(".sh$", "", $0); print $0 }'`
WORKDIR=`realpath $(dirname $0)/../run/$NAME`
DIST_IMAGE="scalac/postgres"
DOCKER_NAME="$PROJECT-$NAME"

function pulled? {
  docker images | grep -E "^$1\s*"
}

function running? {
  docker inspect $1 2> /dev/null | grep Running | grep true
}

function exists? {
  docker ps -a | grep -E " $1\s*$"
}

function failed? {
  docker inspect $1 2> /dev/null | grep ExitCode | awk '{ gsub(",", "", $2); print $2 }'
}

function build {
  docker pull scalac/postgres
}

function start {
  if [ -z "$(pulled? $DIST_IMAGE)" ]; then
    build
  fi

  docker run -d \
    -v $PROJECT_PATH/docker/init.sql:/tmp/init.sql \
	  -p 15432:5432 \
	  --name $DOCKER_NAME \
	  $DIST_IMAGE \
    > /dev/null
}

function stop {
	docker stop $DOCKER_NAME > /dev/null
}

function status {
  if [ -n "$(running? $DOCKER_NAME)" ]; then
		echo "$DOCKER_NAME is started."
	else
    if [ $(failed? $DOCKER_NAME) -gt 0 ]; then
      echo "$DOCKER_NAME failed."
    else
	    echo "$DOCKER_NAME is stopped."
    fi
	fi
}

function clean {
  return 0
}

function clean-dist {
  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    $DIST_IMAGE \
    bash -c "rm -rf /src/docker/run/$NAME"
}

case $1 in
	start)
    if [ -n "$(exists? $DOCKER_NAME)" ]; then
      if [ -n "$(running? $DOCKER_NAME)" ]; then
				echo "$DOCKER_NAME is already running."
				exit 0
			else
				docker start $DOCKER_NAME > /dev/null
			fi
		else
			start
		fi
	;;
	stop)
    if [ -n "$(exists? $DOCKER_NAME)" ]; then
      if [ -n "$(running? $DOCKER_NAME)" ]; then
        stop && \
          echo "$DOCKER_NAME is stopped."
      else
        echo "$DOCKER_NAME is already stopped."
      fi
    else
	    echo "$DOCKER_NAME does not exist."
    fi
	;;
	status)
		status
	;;
	remove)
    if [ -n "$(exists? $DOCKER_NAME)" ]; then
      if [ -n "$(running? $DOCKER_NAME)" ]; then
        echo "$DOCKER_NAME is still running."
  			exit 1
      else
        docker rm $DOCKER_NAME > /dev/null && \
          echo "$DOCKER_NAME removed."
      fi
    else
      echo "$DOCKER_NAME does not exist."
    fi
	;;
  build)
    build
  ;;
  clean-dist)
    clean-dist
  ;;
	*)
	echo "Usage: $0 [start|stop|status|remove|build|clean|clean-dist]"
	exit 1
	;;
esac
