FROM java:8-jre
MAINTAINER Dmirtiy Gorchakov <javaronok.gda@gmail.com>

ADD ./target/kafka-mgd-full.jar /app/kafka-mgd-full.jar
ADD docker/kafka_2.12-1.1.0.tgz /kafka
ADD docker/producer/run.sh /run.sh
ADD docker/producer/start.sh /start.sh
ADD docker/producer/create_topic.sh /kafka/create_topic.sh
ADD docker/producer/describe_topic.sh /kafka/describe_topic.sh
ADD docker/producer/describe_group.sh /kafka/describe_group.sh
RUN chmod a+x /start.sh && \
    chmod a+x /run.sh && \
    chmod a+x /kafka/create_topic.sh && \
    chmod a+x /kafka/describe_topic.sh && \
    chmod a+x /kafka/describe_group.sh

#ENTRYPOINT /start.sh
CMD ["/run.sh"]
