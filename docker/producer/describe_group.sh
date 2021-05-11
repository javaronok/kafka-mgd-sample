#!/bin/sh

kafka_2.12-2.3.1/bin/kafka-consumer-groups.sh --bootstrap-server kafka1:9092,kafka2:9092,kafka3:9092 --describe --group $1 &