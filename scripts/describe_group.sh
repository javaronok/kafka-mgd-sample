#!/usr/bin/env bash
kafka_2.12-0.10.2.1/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group $1 &