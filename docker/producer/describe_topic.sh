#!/bin/sh

/kafka/kafka_2.12-1.1.0/bin/kafka-topics.sh --describe --zookeeper zookeeper:2181 --topic $1 &
