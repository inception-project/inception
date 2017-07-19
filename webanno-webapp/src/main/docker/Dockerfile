############################################################
# Dockerfile to build webanno container images
#
# Inspired by
# * https://github.com/NLPbox/webanno-docker
# * https://github.com/simonmeoni/webanno-docker
############################################################

################## BEGIN INSTALLATION ######################
FROM tomcat:8-jre8

MAINTAINER WebAnno Team

RUN apt-get update
# Install tomcat utilities (we will need tomcat8-instance-create) and mysql
#RUN DEBIAN_FRONTEND=noninteractive apt-get install -y wget mysql-server mysql-client tomcat8-user tomcat8-admin authbind
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y wget tomcat8-user tomcat8-admin authbind

# Copy webanno and mysql settings to tmp
# COPY create_webanno_db.sql mysql-init /tmp/

# Setup mysql as depicted in the webanno admin guide
# RUN service mysql stop
# RUN mysqld_safe --init-file=/tmp/mysql-init &
# RUN rm /tmp/mysql-init
# RUN service mysql start && \
#     mysql -u root < /tmp/create_webanno_db.sql

WORKDIR /opt

# Create a tomcat8 instance of webanno to operate on port 18080 as
# shown in the official admin guide
RUN tomcat8-instance-create -p 18080 -c 18005 webanno && \
    chown -R www-data /opt/webanno

# create some folders to get rid of tomcat8 warnings
RUN mkdir -p /usr/share/tomcat8/common/classes /usr/share/tomcat8/server/classes /usr/share/tomcat8/shared/classes

# Rename the webanno webapp 
COPY webanno_initd /etc/init.d/webanno
COPY @docker.warfile@ /opt/webanno/webapps/ROOT.war

# Setup webanno as a service 
RUN chmod +x /etc/init.d/webanno
RUN update-rc.d webanno defaults

RUN mkdir /export
RUN chown -R www-data /export
# COPY settings.properties /export/settings.properties

COPY entrypoint.sh /opt/webanno/entrypoint.sh

##################### INSTALLATION END #####################

VOLUME /export

EXPOSE 18080

CMD bash /opt/webanno/entrypoint.sh
