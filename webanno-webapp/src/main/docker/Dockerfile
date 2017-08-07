############################################################
#
# Dockerfile to build webanno container images
#
############################################################

FROM openjdk:8

MAINTAINER WebAnno Team

WORKDIR /opt/webanno

COPY @docker.warfile@ webanno-standalone.jar

# this will be the WebAnno home folder
RUN mkdir /export
VOLUME /export

EXPOSE 8080

ENV JAVA_OPTS="-Xmx750m"

CMD java ${JAVA_OPTS} -Djava.awt.headless=true -XX:+UseConcMarkSweepGC -Dwebanno.home=/export -jar webanno-standalone.jar
