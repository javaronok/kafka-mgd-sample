#!/bin/sh

kafka_2.12-0.10.2.1/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic $1 --partitions $2 &
