#!/bin/bash

/kafka/create_topic.sh

if [[ -z "$BROKER_LIST" ]]; then
    export BROKER_LIST=localhost:9092
fi

echo "BROKER_LIST: " ${BROKER_LIST}

java -Xmx200m -jar /app/kafka-mgd-full.jar producer -delay 100 -amount 1000 -brokers ${BROKER_LIST}