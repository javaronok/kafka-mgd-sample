package com.mapr.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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
 * This program reads messages from two topics. Messages on "fast-messages"
 */
public class Consumer {
    @Option(name = "-brokers", usage = "Kafka brokers list (delimiter ',')")
    private String brokers = "localhost:9092";

    public static void main(String[] args) throws IOException {
        Consumer consumer = new Consumer();
        CmdLineParser parser = new CmdLineParser(consumer);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException ce) {
            System.err.println(ce.getMessage());
            System.err.println();
            System.err.println(" Options are:");
            parser.printUsage(System.err); // print the list of available options
            System.err.println();
            System.exit(0);
        }

        consumer.run();
    }

    private void run() throws IOException {
        run(brokers);
    }

    private void run(String brokers) throws IOException {
        // set up house-keeping
        final ObjectMapper mapper = new ObjectMapper();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 1; i++) {
            executor.submit((Runnable) () -> {
                // and the consumer
                try {
                    consume(mapper, brokers);
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

    private void consume(ObjectMapper mapper, String brokers) throws IOException {
        KafkaConsumer<String, String> consumer;
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            if (properties.getProperty("group.id") == null) {
                properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
            }
            if (brokers != null && !brokers.isEmpty()) {
                properties.put("bootstrap.servers", brokers);
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
