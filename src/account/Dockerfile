FROM amazoncorretto:17
ARG APP_HOME=/opt/otus-homework
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
COPY build/libs/otus-homework-9-account.jar $APP_HOME/otus-homework-9-account.jar
EXPOSE 8000
ENTRYPOINT ["java","-jar","otus-homework-9-account.jar"]
