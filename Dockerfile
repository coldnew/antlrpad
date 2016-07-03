FROM java:jre

EXPOSE 9000

RUN mkdir /app
WORKDIR /app

ADD target/universal/antlrpad-play-1.0-SNAPSHOT.tgz /app

RUN chmod a+x antlrpad-play-1.0-SNAPSHOT/bin/antlrpad-play
ENTRYPOINT antlrpad-play-1.0-SNAPSHOT/bin/antlrpad-play -Dplay.crypto.secret=e9a7825e-4144-11e6-9475-f45c89b0e36f -J-Xmx64m