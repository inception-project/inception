############################################################
#
# Dockerfile to build INCEpTION container images
#
############################################################

FROM eclipse-temurin:21-jre@sha256:bca347dc76e38a60a1a01b29a7d1312e514603a97ba594268e5a2e4a1a0c9a8f

LABEL maintainer="INCEpTION Team"

# Define network ports
EXPOSE 8080

# User and group IDs
ENV APP_USER=inception
ENV APP_UID=2000
ENV APP_GROUP=inception
ENV APP_GID=2000

# Language settings
ENV LANG en_US.UTF-8

# Application settings
ENV JAVA_OPTS=""
ENV JAVA_MEM_OPTS="-XX:MaxRAMPercentage=80"
ENV APP_ARGS=""

# Install application JAR
WORKDIR /opt/inception
COPY @docker.jarfile@ inception-app-standalone.jar
COPY launch.sh .

# Update to latest package versions
# Make sure INCEpTION is running in en_US.UTF-8 locale
# Set up user, group and home folder permissions
# Set up application data folder
RUN set -ex \
    && DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get install -y --no-install-recommends locales \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && dpkg-reconfigure --frontend=noninteractive locales \
    && update-locale LANG=en_US.UTF-8 \
    && groupadd -g ${APP_GID} ${APP_GROUP} \
    && useradd \
      -d /opt/inception  \
      -s /bin/bash  \
      -g ${APP_GROUP} -u ${APP_UID} ${APP_USER} \
    && chown -R ${APP_USER}:${APP_GROUP} /opt/inception \
    && chmod +x launch.sh \
    && mkdir /export

VOLUME /export
VOLUME /tmp
VOLUME /var/log

# Launch application
CMD /opt/inception/launch.sh java ${JAVA_MEM_OPTS} ${JAVA_OPTS} -Djava.awt.headless=true -Dinception.home=/export -jar inception-app-standalone.jar ${APP_ARGS}
