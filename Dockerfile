FROM java:jre

EXPOSE 9000

RUN mkdir /app
WORKDIR /app

ADD target/universal/antlrpad-play-1.0-SNAPSHOT.tgz /app

VOLUME /app/data

RUN chmod a+x antlrpad-play-1.0-SNAPSHOT/bin/antlrpad-play
ENTRYPOINT ["antlrpad-play-1.0-SNAPSHOT/bin/antlrpad-play"]