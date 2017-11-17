#!/bin/bash

if [[ -z "$BROKER_LIST" ]]; then
    export BROKER_LIST=localhost:9092
fi

if [[ -z "$THREADS" ]]; then
    export THREADS=1
fi

sleep 60

echo "BROKER_LIST: " ${BROKER_LIST}
echo "THREADS: " ${THREADS}

java -Xmx200m -jar /app/kafka-mgd-full.jar consumer -threads ${THREADS} -brokers ${BROKER_LIST}
