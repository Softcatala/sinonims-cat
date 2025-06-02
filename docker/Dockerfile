FROM maven:3.9.6-eclipse-temurin-17 as build

COPY server /tmp/server

RUN mvn -f /tmp/server/pom.xml clean package

RUN mv /tmp/server/target/sinonims-0.0.1-SNAPSHOT-jar-with-dependencies.jar /tmp/sinonims.jar

FROM maven:3.9.6-eclipse-temurin-17 as prod

COPY dict /dict
COPY server-cfg /srv

COPY --from=build /tmp/sinonims.jar /srv/sinonims.jar

RUN java -jar /srv/sinonims.jar --config /srv/sinonims-prebuild.cfg

ENTRYPOINT /srv/entrypoint.sh
