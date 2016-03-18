#!/bin/bash

VERSION="1.0.0"
PROJECT=`basename $(realpath $(dirname $0)/../..)`
PROJECT_PATH=$(realpath $(dirname $0)/../..)
NAME=`basename $0 | awk '{ gsub(".sh$", "", $0); print $0 }'`
WORKDIR=`realpath $(dirname $0)/../run/$NAME`
BUILD_IMAGE="scalac/activator"
DIST_IMAGE="scalac/octopus"
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
  if [ -z "$(pulled? $BUILD_IMAGE)" ]; then
    docker pull $BUILD_IMAGE
  fi
 
  if [ ! -d $WORKDIR/ivy2/cache ]; then
    mkdir -p $WORKDIR/ivy2
    [ -d ~/.ivy2 ] && \
      cp -R ~/.ivy2/* $WORKDIR/ivy2
  fi

  local CUR_SECRET=$(grep -E "^play.crypto.secret" $PROJECT_PATH/docker/application.conf | sed -e 's/^play\.crypto\.secret=\"\(.*\)\"$/\1/')
  local NEW_SECRET=""

  if [ -z $CUR_SECRET ]; then
    NEW_SECRET=$(docker run --rm -it \
      -v $PROJECT_PATH:/src \
      -v $WORKDIR/ivy2:/root/.ivy2 \
      $BUILD_IMAGE \
      bash -c "cd /src && \
        activator playGenerateSecret| grep 'new secret:' | sed -e 's/^.*\:\s\(.*\)$/\1/'" | strings)

    sed -i -e "/^play\.crypto\.secret=\"\(.*\)\"$/d" $PROJECT_PATH/docker/application.conf
    echo "play.crypto.secret=\"$NEW_SECRET\"" >> $PROJECT_PATH/docker/application.conf
  fi

  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    -v $WORKDIR/ivy2:/root/.ivy2 \
    $BUILD_IMAGE \
    bash -c "cd /src && \
      activator dist && \
      chown -R $(id -u):$(id -g) /src/client /src/project /src/server /src/shared /root/.ivy2"

  cp $PROJECT_PATH/server/target/universal/server-$VERSION.zip $PROJECT_PATH/docker/src
  docker build -t $DIST_IMAGE -f $PROJECT_PATH/docker/src/Dockerfile.backend $PROJECT_PATH/docker/src
}

function watch {
  if [ -z "$(pulled? $DIST_IMAGE)" ]; then
    build
  fi

  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    -v $WORKDIR/ivy2:/root/.ivy2 \
    -p 9000:9000 \
    $BUILD_IMAGE \
    bash -c "cd /src && \
      activator ~run -Dconfig.file=/src/docker/application.conf; \
      chown -R $(id -u):$(id -g) /src/client /src/project /src/server /src/shared /root/.ivy2"
}

function start {
  if [ -z "$(pulled? $DIST_IMAGE)" ]; then
    build
  fi

  docker run -d \
		-v $PROJECT_PATH/docker/application.conf:/opt/scalac/octopus/conf/application.conf \
		-p 9000:9000 \
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
    $BUILD_IMAGE \
    bash -c "rm -rf /src/docker/run/$NAME"

  rm -f $PROJECT_PATH/docker/src/*.zip
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
  watch)
    watch
  ;;
  build)
    build
  ;;
  clean)
    clean
  ;;
	*)
	echo "Usage: $0 [start|stop|status|remove|watch|build|clean]"
	exit 1
	;;
esac
