#!/bin/bash

if [[ -z "REPLICATION_FACTOR" ]]; then
    export REPLICATION_FACTOR=1
fi

if [[ -z "PARTITIONS" ]]; then
    export PARTITIONS=1
fi

if [[ -z "TOPIC" ]]; then
    export TOPIC=fast-messages
fi

echo "TOPIC:" ${TOPIC}
echo "PARTITIONS:" ${PARTITIONS}
echo "REPLICATION_FACTOR:" ${REPLICATION_FACTOR}

sleep 20

kafka/kafka_2.12-0.11.0.1/bin/kafka-topics.sh --create --zookeeper zookeeper:2181 --replication-factor ${REPLICATION_FACTOR} --partitions ${PARTITIONS} --topic ${TOPIC} --if-not-exists