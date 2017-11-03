FROM java:8-jre
MAINTAINER Dmirtiy Gorchakov <javaronok.gda@gmail.com>

ADD ./target/kafka-mgd-full.jar /app/kafka-mgd-full.jar
CMD ["java", "-Xmx200m", "-jar", "/app/kafka-mgd-full.jar", "producer"]
