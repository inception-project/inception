############################################################
# Dockerfile to build webanno container images
#
# Inspired by
# * https://github.com/NLPbox/webanno-docker
# * https://github.com/simonmeoni/webanno-docker
############################################################

################## BEGIN INSTALLATION ######################
FROM tomcat:7-jre8

MAINTAINER WebAnno Team

RUN apt-get update
# Install tomcat utilities (we will need tomcat7-instance-create) and mysql
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y wget mysql-server mysql-client tomcat7-user tomcat7-admin authbind

# Copy webanno and mysql settings to tmp
COPY create_webanno_db.sql mysql-init /tmp/

# Setup mysql as depicted in the webanno admin guide
RUN service mysql stop
RUN mysqld_safe --init-file=/tmp/mysql-init &
RUN rm /tmp/mysql-init
RUN service mysql start && \
    mysql -u root < /tmp/create_webanno_db.sql

WORKDIR /opt

# Create a tomcat7 instance of webanno to operate on port 18080 as
# shown in the official admin guide
RUN tomcat7-instance-create -p 18080 -c 18005 webanno && \
    chown -R www-data /opt/webanno

# create some folders to get rid of tomcat7 warnings
RUN mkdir -p /usr/share/tomcat7/common/classes /usr/share/tomcat7/server/classes /usr/share/tomcat7/shared/classes

# Rename the webanno webapp 
COPY webanno_initd /etc/init.d/webanno
COPY @docker.warfile@ /opt/webanno/webapps/ROOT.war

# Setup webanno as a service 
RUN chmod +x /etc/init.d/webanno
RUN update-rc.d webanno defaults
RUN mkdir /srv/webanno

COPY settings.properties /srv/webanno/settings.properties
RUN chown -R www-data /srv/webanno

COPY entrypoint.sh /opt/webanno/entrypoint.sh

##################### INSTALLATION END #####################

VOLUME /srv/webanno/repository
VOLUME /var/lib/mysql

EXPOSE 18080

CMD bash /opt/webanno/entrypoint.sh
