#!/usr/bin/env bash
kafka_2.12-2.3.1/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group $1 &