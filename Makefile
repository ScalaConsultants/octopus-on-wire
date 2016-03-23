MODULES=database backend
WORKDIR=$$PWD/docker/run
LINUX_DEPS=
MACOSX_DEPS=coreutils docker-machine-nfs
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
		[ -f /usr/local/bin/tac ] || \
			ln -s /usr/local/bin/gtac /usr/local/bin/tac 2>/dev/null; \
		grep Users /etc/exports > /dev/null || \
			docker-machine-nfs default; \
	fi

init: deps
	@echo "\033[1mInitializing directories structure...\033[0m"
	@for NAME in $(MODULES); do \
		if [ ! -d $(WORKDIR)/$$NAME ]; then \
			echo `basename $(WORKDIR)`/$$NAME; \
			mkdir -p $(WORKDIR)/$$NAME; \
		fi \
	done

startup: build
	@echo "\033[1mInitializing docker containers...\033[0m"
	@for NAME in $(MODULES); do \
		docker/bin/$$NAME.sh start; \
	done && \
	for NAME in $(MODULES); do \
		docker/bin/$$NAME.sh status; \
	done

shutdown:
	@echo "\033[1mShutting down docker containers...\033[0m"
	@for NAME in `echo $(MODULES) | tr ' ' '\n' | tac`; do \
		docker/bin/$$NAME.sh stop; \
	done

remove: shutdown
	@echo "\033[1mRemoving docker containers...\033[0m"
	@for NAME in `echo $(MODULES) | tr ' ' '\n' | tac`; do \
		docker/bin/$$NAME.sh remove; \
	done

clean: remove
	@echo "\033[1mCleaning...\033[0m"
	@for NAME in `echo $(MODULES) | tr ' ' '\n' | tac`; do \
		docker/bin/$$NAME.sh clean; \
	done

clean-dist: remove
	@echo "\033[1mRemoving directories structure...\033[0m"
	@for NAME in `echo $(MODULES) | tr ' ' '\n' | tac`; do \
		if [ -d $(WORKDIR)/$$NAME ]; then \
			echo `basename $(WORKDIR)`/$$NAME; \
			docker/bin/$$NAME.sh "clean-dist"; \
		fi \
	done

watch: init remove
	@echo "\033[1mWatching for changes...\033[0m"
	@docker/bin/database.sh start && \
		docker/bin/database.sh status
	@docker/bin/backend.sh watch
	@docker/bin/database.sh stop

build: init remove
	@echo "\033[1mBuilding image...\033[0m"
	@for NAME in $(MODULES); do \
		docker/bin/$$NAME.sh build; \
	done
