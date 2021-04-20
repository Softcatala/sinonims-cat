FROM maven:3-jdk-11 as build

COPY server /tmp/server

RUN mvn -f /tmp/server/pom.xml clean package

RUN mv /tmp/server/target/sinonims-0.0.1-SNAPSHOT-jar-with-dependencies.jar /tmp/sinonims.jar

FROM adoptopenjdk:11-jre as prod

COPY dict /dict
COPY server-cfg /srv

COPY --from=build /tmp/sinonims.jar /srv/sinonims.jar

ENTRYPOINT /srv/entrypoint.sh
