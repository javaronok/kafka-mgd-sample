#!/bin/sh

kafka_2.12-0.11.0.1/bin/kafka-topics.sh --describe --zookeeper zookeeper:2181 --topic $1 &
