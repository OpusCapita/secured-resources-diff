FROM opuscapita/minsk-core-oracle-jre:1.8.192

ARG NAME
ARG BUILD_DATE
ARG VCS_URL
ARG VCS_REF
ARG VERSION

LABEL maintainer="Alexey Sergeev <alexey.sergeev@opuscapita.com>" \
      org.label-schema.name=$NAME \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.vcs-url=$VCS_URL \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vendor="OpusCapita Group Oy" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# Download Tomcat from Apache website
RUN curl -fLk -o /tmp/tomcat.tar.gz https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.92/bin/apache-tomcat-7.0.92.tar.gz
# Tomcat Home directory
ENV TOMCAT_HOME=/usr/lib/tomcat
# extract tomcat into TOMCAT_HOME folder
RUN mkdir -p $TOMCAT_HOME && tar -xzf /tmp/tomcat.tar.gz --strip-components=1 -C $TOMCAT_HOME
# remove all deafult applications
RUN rm -rf $TOMCAT_HOME/webapps/*

ARG WAR_PATH
ENV APPLICATION_PATH=/usr/lib/application
RUN mkdir -p $APPLICATION_PATH
# copywar file
COPY $WAR_PATH ${APPLICATION_PATH}/app.war
# extract application by APPLICATION_PATH and remove war file
RUN unzip -q -x $APPLICATION_PATH/app.war -d $APPLICATION_PATH && rm ${APPLICATION_PATH}/app.war

COPY .docker/entrypoint.sh /usr/bin/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["entrypoint.sh"]
