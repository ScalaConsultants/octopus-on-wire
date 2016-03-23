#!/bin/bash

VERSION="1.0.0"
PROJECT=`basename $(realpath $(dirname $0)/../..)`
PROJECT_PATH=$(realpath $(dirname $0)/../..)
NAME=`basename $0 | awk '{ gsub(".sh$", "", $0); print $0 }'`
WORKDIR=`realpath $(dirname $0)/../run/$NAME`
BUILD_IMAGE="scalac/activator"
DIST_IMAGE="scalac/octopus"
DOCKER_NAME="$PROJECT-$NAME"
OSTYPE=`uname -s`

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

function set_unchanged_files {
  git update-index --assume-unchanged docker/application.conf
  git update-index --assume-unchanged shared/src/main/scala/scalac/octopusonwire/shared/config/SharedConfig.scala
}

function set_database_host {
  if [ -z $DB_HOST ]; then
    if [ -n $DOCKER_HOST ]; then
      DB_HOST=`echo $DOCKER_HOST | sed -e "s/tcp:\/\/\(.*\):.*/\1/"`
    else
      DB_HOST=`ifconfig docker0 | grep "inet addr" | awk '{ split($2,a,":"); print a[2]; }'`
    fi
  fi

  if [ -n $DB_HOST ]; then
    sed -i -e "s/\(url = \".*:\/\/\).*\(:.*\"\)/\1$DB_HOST\2/" \
      $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
  fi
}

function set_database_port {
  if [ -z $DB_PORT ]; then
    DB_PORT=15432
  fi

  if [ -n $DB_PORT ]; then
    sed -i -e "s/\(url = \".*:\/\/.*:\).*\(\/.*\"\)/\1$DB_PORT\2/" \
      $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
  fi
}

function set_database_name {
  if [ -z $DB_NAME ]; then
    DB_NAME="octopus"
  fi

  if [ -n $DB_NAME ]; then
    sed -i -e "s/\(url = \".*:\/\/.*:.*\/\).*\(\"\)/\1$DB_NAME\2/" \
      $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
  fi
}

function set_database_user {
  if [ -z $DB_USER ]; then
    DB_USER="octopus"
  fi

  if [ -n $DB_USER ]; then
    sed -i -e "s/\(username = \"\).*\(\"\)/\1$DB_USER\2/" \
      $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
  fi
}

function set_database_pass {
  if [ -z $DB_PASS ]; then
    DB_PASS="octopus"
  fi

  if [ -n $DB_PASS ]; then
    sed -i -e "s/\(password = \"\).*\(\"\)/\1$DB_PASS\2/" \
      $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
  fi
}

function set_backend_domain {
  if [ -z $BACKEND_DOMAIN ]; then
    BACKEND_DOMAIN="127.0.0.1"
    if [ -n $DOCKER_HOST ]; then
      BACKEND_DOMAIN=`echo $DOCKER_HOST | sed -e "s/tcp:\/\/\(.*\):.*/\1/"`
    fi
  fi

  if [ -n $BACKEND_DOMAIN ]; then
    sed -i -e "s/\(BackendDomain = \"\).*\(\"\)/\1$BACKEND_DOMAIN\2/" \
      $PROJECT_PATH/shared/src/main/scala/scalac/octopusonwire/shared/config/SharedConfig.scala
    rm -f $PROJECT_PATH/shared/src/main/scala/scalac/octopusonwire/shared/config/SharedConfig.scala-e 2> /dev/null
  fi
}

function fix_perms {
  if [ $OSTYPE = "Linux" ]; then
    docker run --rm -it \
      -v $PROJECT_PATH:/src \
      -v $WORKDIR/ivy2:/root/.ivy2 \
      $BUILD_IMAGE \
      bash -c "chown -R $(id -u):$(id -g) /src/client /src/project /src/server /src/shared /root/.ivy2"
  fi
}

function sync_ivy_cache {
  if [ ! -d $WORKDIR/ivy2/cache ]; then
    mkdir -p $WORKDIR/ivy2
    [ -d ~/.ivy2 ] && \
      cp -R ~/.ivy2/* $WORKDIR/ivy2
  fi
}

function build {
  if [ -z "$(pulled? $BUILD_IMAGE)" ]; then
    docker pull $BUILD_IMAGE
  fi

  sync_ivy_cache
  set_unchanged_files

  local CUR_SECRET=$(grep -E "^play.crypto.secret" $PROJECT_PATH/docker/application.conf | sed -e 's/^play\.crypto\.secret = \"\(.*\)\"$/\1/')
  local NEW_SECRET=""

  if [ -z $CUR_SECRET ]; then
    NEW_SECRET=$(docker run --rm -it \
      -v $PROJECT_PATH:/src \
      -v $WORKDIR/ivy2:/root/.ivy2 \
      $BUILD_IMAGE \
      bash -c "cd /src && \
        activator playGenerateSecret| grep 'new secret:' | sed -e 's/^.*\:\s\(.*\)$/\1/'" | strings)

    sed -i -e "/^play\.crypto\.secret = \"\(.*\)\"$/d" $PROJECT_PATH/docker/application.conf
    rm -f $PROJECT_PATH/docker/application.conf-e 2> /dev/null
    echo "play.crypto.secret=\"$NEW_SECRET\"" >> $PROJECT_PATH/docker/application.conf
  fi

  set_backend_domain

  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    -v $WORKDIR/ivy2:/root/.ivy2 \
    $BUILD_IMAGE \
    bash -c "cd /src && \
      activator dist"

  fix_perms

  cp $PROJECT_PATH/server/target/universal/server-$VERSION.zip $PROJECT_PATH/docker/src
  docker build -t $DIST_IMAGE -f $PROJECT_PATH/docker/src/Dockerfile $PROJECT_PATH/docker/src
}

function watch {
  if [ -z "$(pulled? $BUILD_IMAGE)" ]; then
    docker pull $BUILD_IMAGE
  fi

  sync_ivy_cache
  set_unchanged_files
  set_backend_domain
  set_database_host
  set_database_port
  set_database_name
  set_database_user
  set_database_pass

  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    -v $WORKDIR/ivy2:/root/.ivy2 \
    -p 9000:9000 \
    $BUILD_IMAGE \
    bash -c "cd /src && \
      activator ~run -Dconfig.file=/src/docker/application.conf"

  fix_perms
}

function start {
  if [ -z "$(pulled? $DIST_IMAGE)" ]; then
    build
  fi

  set_unchanged_files
  set_backend_domain
  set_database_host
  set_database_port
  set_database_name
  set_database_user
  set_database_pass

  docker run -d \
		-v $PROJECT_PATH/docker/application.conf:/opt/scalac/octopus/conf/application.conf \
		-p 9000:9000 \
		--name $DOCKER_NAME \
	  $DIST_IMAGE \
    > /dev/null
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
    -v $WORKDIR/ivy2:/root/.ivy2 \
    $BUILD_IMAGE \
    bash -c "cd src && \
      activator clean"
}

function clean-dist {
  docker run --rm -it \
    -v $PROJECT_PATH:/src \
    -v $WORKDIR/ivy2:/root/.ivy2 \
    $BUILD_IMAGE \
    bash -c "cd src && \
      rm -rf /src/docker/run/$NAME"

  rm -f $PROJECT_PATH/docker/src/*.zip
  docker images | grep -E "^$DIST_IMAGE " > /dev/null && \
  docker rmi $DIST_IMAGE

  git checkout docker/application.conf
  git checkout shared/src/main/scala/scalac/octopusonwire/shared/config/SharedConfig.scala
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
  watch)
    watch
  ;;
  build)
    build
  ;;
  clean)
    clean
  ;;
  clean-dist)
    clean-dist
  ;;
	*)
	echo "Usage: $0 [start|stop|status|remove|watch|build|clean|clean-dist]"
	exit 1
	;;
esac
