#!/bin/bash

PROJECT=`basename $(realpath $(dirname $0)/../..)`
PROJECT_PATH=$(realpath $(dirname $0)/../..)
NAME=`basename $0 | awk '{ gsub(".sh$", "", $0); print $0 }'`
WORKDIR=`realpath $(dirname $0)/../run/$NAME`
VERSION=""
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
  docker build -t $DIST_IMAGE -f $PROJECT_PATH/docker/src/Dockerfile.$NAME $PROJECT_PATH/docker/src
}

function start {
  if [ -z "$(pulled? $DIST_IMAGE)" ]; then
    build
  fi
  
  if [ ! -d $WORKDIR/etc ]; then
    mkdir $WORKDIR/etc
    docker run --rm \
      -v $WORKDIR/etc:/tmp/workdir \
      $DIST_IMAGE bash -c 'cp -aR /etc/postgresql/* /tmp/workdir'
  fi

  if [ ! -d $WORKDIR/var ]; then
    mkdir $WORKDIR/var
    docker run --rm \
      -v $WORKDIR/var:/tmp/workdir \
      $DIST_IMAGE bash -c 'cp -aR /var/lib/postgresql/* /tmp/workdir'
  fi
 
  docker run -d \
		-v $WORKDIR/etc:/etc/postgresql \
    -v $WORKDIR/var:/var/lib/postgresql \
    -v $PROJECT_PATH/docker/init.sql:/tmp/init.sql \
		-p 15432:5432 \
		--name $DOCKER_NAME \
	  $DIST_IMAGE
}

function stop {
	docker stop $DOCKER_NAME >/dev/null
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
  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    $DIST_IMAGE \
    bash -c "rm -rf /src/docker/run/$NAME"
    docker rmi $DIST_IMAGE
}

case $1 in
	start)
    if [ -n "$(exists? $DOCKER_NAME)" ]; then
      if [ -n "$(running? $DOCKER_NAME)" ]; then
				echo "$DOCKER_NAME is already running."
				exit 0
			else
				docker start $DOCKER_NAME
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
        docker rm $DOCKER_NAME
      fi
    else
      echo "$DOCKER_NAME does not exist."
    fi
	;;
  build)
    build
  ;;
  clean)
    clean
  ;;
	*)
	echo "Usage: $0 [start|stop|status|remove|build|clean]"
	exit 1
	;;
esac
