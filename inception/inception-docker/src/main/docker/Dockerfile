############################################################
#
# Dockerfile to build INCEpTION container images
#
############################################################

FROM eclipse-temurin:17

MAINTAINER INCEpTION Team

# Define network ports
EXPOSE 8080

# make sure INCEpTION is running in en_US.UTF-8 locale
RUN set -ex \
    && DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get install -y --no-install-recommends locales \
    && apt-get upgrade -y

# Set up language
RUN set -ex \
    && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && dpkg-reconfigure --frontend=noninteractive locales \
    && update-locale LANG=en_US.UTF-8
ENV LANG en_US.UTF-8

# Install application JAR
WORKDIR /opt/inception
COPY @docker.jarfile@ inception-app-standalone.jar
COPY launch.sh .

# Set up user, group and home folder permissions
ENV APP_USER=inception
ENV APP_UID=2000
ENV APP_GROUP=inception
ENV APP_GID=2000
RUN groupadd -g ${APP_GID} ${APP_GROUP} \ 
    && useradd \
      -d /opt/inception  \
      -s /bin/bash  \
      -g ${APP_GROUP} -u ${APP_UID} ${APP_USER} \
    && chown -R ${APP_USER}:${APP_GROUP} /opt/inception \
    && chmod +x launch.sh

# Set up application data folder
RUN mkdir /export
VOLUME /export
VOLUME /tmp
VOLUME /var/log

# Launch application
ENV JAVA_OPTS=""
ENV JAVA_MEM_OPTS="-XX:MaxRAMPercentage=80"
ENV APP_ARGS=""
CMD /opt/inception/launch.sh java ${JAVA_MEMORY_OPTS} ${JAVA_OPTS} -Djava.awt.headless=true -Dinception.home=/export -jar inception-app-standalone.jar ${INCEPTION_ARGUMENTS}
