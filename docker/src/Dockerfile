FROM scalac/java8:latest

MAINTAINER Jakub Zubielik <jakub.zubielik@scalac.io>

ENV ROOT_DIR /opt/scalac/octopus/
ENV VERSION 1.0.0

RUN mkdir -p $ROOT_DIR

ADD server-$VERSION.zip $ROOT_DIR

RUN cd $ROOT_DIR && \
    unzip server-$VERSION.zip && \
    mv server-$VERSION/* . && \
    rm -rf server-$VERSION*

RUN mkdir -p /etc/my_init.d

RUN echo '#!/bin/bash\n\
cd $ROOT_DIR\n\
bin/server -Dconfig.file=conf/application.conf' \
> /etc/my_init.d/octopus

RUN chmod +x /etc/my_init.d/octopus

EXPOSE 9000

CMD ["/sbin/my_init"]
