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