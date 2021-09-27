FROM maven:3.8.2-openjdk-17

COPY . /home/li20/java/flex-server
ENV POSTGRES_URL=localhost:5432
ENV SIMULATION_FACTOR=1
ENV SIMULATION_START_TIME=now
ENV FLEX_PROFILE=requests

USER root
RUN cd $JAVA_HOME/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias adminserver -file \
    /home/li20/java/flex-server/src/main/resources/keystore/adminserver2.crt

WORKDIR /home/li20/java/flex-server

ENTRYPOINT mvn clean spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=${FLEX_PROFILE} \
--spring.datasource.url=jdbc:postgresql://${POSTGRES_URL}/flex \
--server.port=8080 \
--de.unik.enavi.market.testing.load=FALSE \
--de.unik.enavi.market.time.factor=${SIMULATION_FACTOR} \
--de.unik.enavi.market.time.basetime=${SIMULATION_START_TIME}"