#!/bin/sh

kafka_2.12-0.10.2.1/bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic $1 &
