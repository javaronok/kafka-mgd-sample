package com.mapr.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This program reads messages from two topics. Messages on "fast-messages" are analyzed
 * to estimate latency (assuming clock synchronization between producer and consumer).
 * <p/>
 * Whenever a message is received on "slow-messages", the stats are dumped.
 */
public class Consumer {
    public static void main(String[] args) throws IOException {
        // set up house-keeping
        final ObjectMapper mapper = new ObjectMapper();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 1; i++) {
            executor.submit((Runnable) () -> {
                // and the consumer
                try {
                    consume(mapper);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();

        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }

    public static void consume(ObjectMapper mapper) throws IOException {
        KafkaConsumer<String, String> consumer;
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            if (properties.getProperty("group.id") == null) {
                properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
            }
            consumer = new KafkaConsumer<>(properties);
        }
        consumer.subscribe(Arrays.asList("fast-messages", "summary-markers"));
        //consumer.assign(Collections.singleton(new TopicPartition("fast-messages", 1)));
        int timeouts = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            // read records with a short timeout. If we time out, we don't really care.
            ConsumerRecords<String, String> records = consumer.poll(10000);
            Thread.yield();
            if (records.count() == 0) {
                timeouts++;
            } else {
                System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
                timeouts = 0;
            }
            for (ConsumerRecord<String, String> record : records) {
                switch (record.topic()) {
                    case "fast-messages":
                        // the send time is encoded inside the message
                        JsonNode msg = mapper.readTree(record.value());
                        switch (msg.get("type").asText()) {
                            case "test":
                                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                                Date date = new Date(msg.get("t").asLong());
                                System.out.printf("Thread: %s, Topic:%s, partition:%d, Value: %d, time: %s \n",
                                        Thread.currentThread().getName(),
                                        record.topic(), record.partition(),
                                        msg.get("k").asInt(), sdf.format(date));
                                break;
                            case "marker":
                                break;
                            default:
                                throw new IllegalArgumentException("Illegal message type: " + msg.get("type"));
                        }
                        break;
                    default:
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }
            consumer.commitSync();
        }
    }
}
