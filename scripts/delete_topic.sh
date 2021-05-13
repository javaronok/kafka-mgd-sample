#!/bin/sh

kafka_2.12-2.3.1/bin/kafka-topics.sh --delete --zookeeper localhost:2181 --topic $1 &
