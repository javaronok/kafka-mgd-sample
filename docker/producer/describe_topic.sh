#!/bin/sh

/kafka/kafka_2.12-2.3.1/bin/kafka-topics.sh --describe --zookeeper zookeeper:2181 --topic $1 &
