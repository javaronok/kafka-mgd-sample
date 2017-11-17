FROM java:8-jre
MAINTAINER Dmirtiy Gorchakov <javaronok.gda@gmail.com>

ADD ./target/kafka-mgd-full.jar /app/kafka-mgd-full.jar
ADD docker/consumer/start.sh /start.sh
RUN chmod a+x /start.sh
#ENTRYPOINT ["/start.sh"]
CMD ["/start.sh"]
#CMD ["java", "-Xmx200m", "-jar", "/app/kafka-mgd-full.jar", "consumer", "-brokers", "${BROKER_LIST}"]
