MODULES=database backend
WORKDIR=$$PWD/docker/run
LINUX_DEPS=
MACOSX_DEPS=realpath
MAKEFLAGS+=--silent
OSTYPE=$(shell uname -s)

all: help

help:

deps:
	@echo "\033[1mInstalling dependencies...\033[0m"
	if [ $(OSTYPE) = "Linux" ]; then \
		for PKG in $(LINUX_DEPS); do \
			dpkg -l | grep $$PKG | grep -c ii >/dev/null || \
			sudo apt-get install -y $$PKG; \
		done; \
	elif [ $(OSTYPE) = "Darwin" ]; then \
		which brew >/dev/null || \
		sh -c "ruby -e \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)\""; \
		for PKG in $(DEPS); do \
			brew list -1 | grep "^$$PKG$$" >/dev/null || \
				brew install $$PKG; \
		done; \
	fi

init: deps
	@echo "\033[1mInitializing directories structure...\033[0m"
	@for NAME in $(MODULES); do \
		if [ ! -d $(WORKDIR)/$$NAME ]; then \
			echo `basename $(WORKDIR)`/$$NAME; \
			mkdir -p $(WORKDIR)/$$NAME; \
		fi \
	done

startup: init
	@echo "\033[1mInitializing docker containers...\033[0m"
	@for NAME in $(MODULES); do \
		docker/bin/$$NAME.sh start | grep running || \
		(sleep 2 && \
		docker/bin/$$NAME.sh status); \
	done

shutdown:
	@echo "\033[1mShutting down docker containers...\033[0m"
	@for NAME in `echo $(MODULES) | sed 's/ /\n/g' | tac`; do \
		docker/bin/$$NAME.sh stop; \
	done

clean: shutdown
	@echo "\033[1mRemoving docker containers...\033[0m"
	@for NAME in `echo $(MODULES) | sed 's/ /\n/g' | tac`; do \
		docker/bin/$$NAME.sh remove; \
	done

clean-dist: shutdown clean
	@echo "\033[1mRemoving directories structure...\033[0m"
	@for NAME in `echo $(MODULES) | sed 's/ /\n/g' | tac`; do \
		if [ -d $(WORKDIR)/$$NAME ]; then \
			echo `basename $(WORKDIR)`/$$NAME; \
			docker/bin/$$NAME.sh clean; \
		fi \
	done

watch: init
	@echo "\033[1mWatching for changes...\033[0m"
	@docker/bin/database.sh start
	@docker/bin/backend.sh watch
	@docker/bin/database.sh stop

build: init
	@echo "\033[1mBuilding image...\033[0m"
	@for NAME in $(MODULES); do \
		docker/bin/$$NAME.sh build; \
	done
