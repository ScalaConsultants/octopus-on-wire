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

Server build and development process is wrapped around `GNU Make` and `Docker`.

**Available targets:**

```bash
make watch      # start environment in DEV mode in the foreground
                # rebuild on file change
                # http://127.0.0.1:9000/plain

make startup    # start environment in PROD mode in the background
                # http://127.0.0.1:9000/plain

make shutdown   # shutdown environment

make clean      # remove conatainers

make clean-dist # remove conatainers and images
                # wipe out database and ivy2 cache
```

**Description**

This bundle contain two modules: `database` and `backend`, which operate in separeted
conantainers named after source root diriectory name and module name defined in `Makefile`,
for ex.: `project-module`.

`make` will build two images: `scalac/postgres` and `scalac/octopus`, next based on that
images it will start `octopus-on-wire-database` and `octopus-on-wire-backend` containers.

Configuration file for `octopus-on-wire-backend` can be found at `docker/application.conf`.
It's automatically generated and used only by the containers. Never push your configuration
file back in to the repository because it contains `play.crypto.secret` value. Initially
this value is empty and generated automatically during build process. This file was also
added to `.gitignore` to avoid accidental disclosure.

To speed up build process `octopus-on-wire-backend` will copy your existing `~/.ivy2`
folder to `docker/run/backend/ivy2` and use this folder as a persistant cache during 
container restarts.

To preserve database content between restarts `octopus-on-wire-database` will use
`docker/run/database` as a persistant data storage.

`octopus-on-wire-database` executes `docker/init.sql` file. Use it to add extra SQL,
for ex.: test data or database configuration.

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
