############################################################
#
# Dockerfile to build WebAnno container images
#
############################################################

FROM openjdk:11

MAINTAINER WebAnno Team

# make sure WebAnno is running in en_US.UTF-8 locale
RUN set -ex \
      && DEBIAN_FRONTEND=noninteractive \
      && apt-get update \
      && apt-get install -y --no-install-recommends locales
RUN set -ex \
      && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
      && dpkg-reconfigure --frontend=noninteractive locales \
      && update-locale LANG=en_US.UTF-8
ENV LANG en_US.UTF-8

WORKDIR /opt/webanno

COPY @docker.jarfile@ webanno-standalone.jar

# this will be the WebAnno home folder
RUN mkdir /export
VOLUME /export

EXPOSE 8080

ENV JAVA_OPTS="-Xmx750m"

CMD java ${JAVA_OPTS} -Djava.awt.headless=true -Dwebanno.home=/export -jar webanno-standalone.jar
