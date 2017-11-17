FROM java:8-jre
MAINTAINER Dmirtiy Gorchakov <javaronok.gda@gmail.com>

ADD ./target/kafka-mgd-full.jar /app/kafka-mgd-full.jar
ADD docker/producer/run.sh /run.sh
ADD docker/producer/start.sh /start.sh
ADD docker/producer/create_topic.sh /create_topic.sh
ADD docker/kafka_2.12-0.11.0.1.tgz /kafka
RUN chmod a+x /start.sh && \
    chmod a+x /run.sh && \
    chmod a+x /create_topic.sh
#ENTRYPOINT /start.sh
CMD ["/run.sh"]
