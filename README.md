# octopus-on-wire

![From http://bgott.blogspot.com/2005_06_01_archive.html](http://www.barrygott.com/blog/flamingocto.jpg)

# Scala event calendar widget

A JavaScript widget for displaying upcoming Scala events on your website in a fully customizable widget.

# About

- Public Name: Event Rocket
- Type: Growth, (possibly) Startup
- Status: Crawling, pre-MVP
- Goals for 2016: Deploy an MVP
- Description: A distributed network of widgets promoting technical events. Serves information to website visitors in a similar fassion as online adverts do (but without being intrusive) - using the reach of our affiliates
- Value to Scalac: Promoting our events, Gathering data for Huntly

# Client usage

**TODO**

# Server usage

### The easy way

**Dependencies**

You will need to install [Docker](https://docs.docker.com/linux/step_one/) (on Linux) or [Docker Toolbox](https://docs.docker.com/linux/step_one/) (on OSX)) manually.<br/>
 `make` will install other dependencies automaticaly asking you about your password if it's needed.

OSX dependencies:
- brew
- coreutils
- docker-machine-nfs

OSX notes:
- To speed up builds tweak CPU and RAM settings of the `boot2docker` VM.

**Usage**

```bash
make build      # builds `scalac/octopus` image

make watch      # an alias for `activator ~run`

make startup    # starts `backend` and `database` containers in a background

make shutdown   # stops containers

make remove     # removes containers

make clean      # an alias for `activator clean`

make clean-dist # removes `backend` and `database` conatainers
                # removes `scalac/octopus` image
                # restores initial settings (removes `play.crypto.secret`!)
```

Deployed application will be available at `http://${BACKEND_DOMAIN}:9000/plain`.

**Environment variables**

You can override default configuration using environment variables.

```bash
BACKEND_DOMAIN  # overrides SharedConfig.BackendDomain
DB_HOST         # overrides database address
DB_PORT         # overrides database port
DB_NAME         # overrides database name
DB_USER         # overrides database user
DB_PASS         # overrides database password
```

For ex.:

```bash
BACKEND_DOMAIN=app.example.com \
DB_HOST=db.example.com \
DB_PORT=5432 \
DB_NAME=octopus \
DB_USER=octopus \
DB_PASS=octopus \
make startup
```

By default `BACKEND_DOMAIN` is set to `127.0.0.1` on Linux or `$(docker-machine ip default)` on OSX.<br/>
By default `DB_HOST` is set docker0 interface IP (172.17.42.1) on Linux or `$(docker-machine ip default)` on OSX.<br/>
To avoid conflicts with existing database `DB_PORT` is set to `15434` by default.

**Description**

This bundle contain two modules: `database` and `backend` which operate in separete conantainers.
Containers are named after source root diriectory and module name defined in `Makefile`, for ex.: `${PROJECT_NAME}-${MODULE}`.

`make` will pull `scalac/postgres` and build `scalac/octopus` images, next base on that
images it will start `octopus-on-wire-database` and `octopus-on-wire-backend` containers.

`make` will take care for starting and stopping components in the right order, so whenever
you run for ex.: `startup` or `watch` it will execute all required tasks to find itself in the desired state.

Configuration for `octopus-on-wire-backend` can be found in `docker/application.conf` file.
It's automatically generated and used only by the containers. Never push your configuration
file back in to the repository because it contains `play.crypto.secret` value. Initially
this value is empty and generated automatically during build process. This file was marked 
as `assume-unchanged` and added to `.gitignore` to avoid accidental disclosure.

`shared/src/main/scala/scalac/octopusonwire/shared/config/SharedConfig.scala` is changed
during build in order to override `SharedConfig.BackendDomain` by `BACKEND_DOMAIN` environment variable.
This file was also marked as `assume-unchanged` and added to `.gitignore`.

To speed up build process `octopus-on-wire-backend` will copy your existing `~/.ivy2`
folder to `docker/run/backend/ivy2` and use this folder as a persistant cache during builds.

`octopus-on-wire-database` executes `docker/init.sql` and `docker/init.sh` files on start.
You can use those files to override default settings.

### The hard way

You'll need a running PostgreSQL instance.

For development, the settings are as follows:

	db_name = event_rocket
	port = 5432
	user = root
	password = <empty>
	
You can change these defaults in `server/src/main/resources/application.conf`.

Once you have the database up and running, you can go on to run the application:

	sbt run
	
or use IntelliJ IDEA's Play configuration to run the app.

# Setup database

### On Mac OS X

Install postgres

	brew install postgresql
	
Run postgres
	
	cd /usr/local/var/postgres
	pg_ctl start -D . -l log

Create user

	createuser root

Create database
	
	createdb event_rocket
	
Connect to database

	psql -e event_rocket	
