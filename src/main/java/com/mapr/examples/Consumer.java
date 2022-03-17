package com.mapr.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.consumer.AvroKafkaConsumer;
import com.mapr.consumer.KafkaMessageConsumer;
import com.mapr.consumer.PlainTextKafkaConsumer;
import com.mapr.tracing.TracingService;
import com.mapr.tracing.TracingSpan;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This program reads messages from two topics. Messages on "fast-messages"
 */
public class Consumer {
    @Option(name = "-brokers", usage = "Kafka brokers list (delimiter ',')")
    private String brokers = "localhost:9092";

    @Option(name = "-registry", usage = "Schema registry URL")
    private String schemaRegistry = "localhost:8081";

    @Option(name = "-threads", usage = "Kafka consumer threads number")
    private Integer threads = 1;

    @Option(name = "-avro", usage = "Avro serialization")
    private Boolean avro = false;

    private Statistics statistics = new Statistics();

    public static void main(String[] args) {
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

    private void run() {
        run(brokers, threads);
    }

    private void run(String brokers, Integer threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ExecutorService stat = Executors.newSingleThreadExecutor();

        for (int i = 0; i < threads-1; i++) {
            KafkaMessageConsumer c = createKafkaConsumer(brokers, "fast-messages", schemaRegistry);
            Runnable consumer = consumeJob(c);
            executor.submit(consumer);
        }

        KafkaMessageConsumer c = new PlainTextKafkaConsumer(brokers, "summary-stat");
        Runnable consumer = consumeJob(c);
        stat.submit(consumer);

        executor.shutdown();
        stat.shutdown();

        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
            stat.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }

    private KafkaMessageConsumer createKafkaConsumer(String brokers, String topic, String schemaRegistry) {
        return avro != null && avro
                ? new AvroKafkaConsumer(brokers, topic, schemaRegistry)
                : new PlainTextKafkaConsumer(brokers, topic);
    }

    private Runnable consumeJob(KafkaMessageConsumer c) {
        // set up house-keeping
        return () -> {
            // and the consumer
            try {
                c.consume(this::consumeTopicMessage);
            } catch (Exception e) {
                System.out.printf("%s", e);
            }
        };
    }

    public void consumeTopicMessage(String topic, int partition, String key, String value, ObjectMapper mapper, TracingService tracer) throws Exception {
        switch (topic) {
            case "fast-messages":
                // the send time is encoded inside the message
                JsonNode msg = mapper.readTree(value);
                switch (msg.get("type").asText()) {
                    case "test":
                        String traceId = msg.get("traceId").asText();
                        try (TracingSpan consumeSpan = tracer.createSpanFromRemote("consumer", traceId, null, false)) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                            Date date = new Date(msg.get("t").asLong());
                            System.out.printf("Thread: %s, Topic:%s, partition:%d, Value: %d, time: %s \n",
                                    Thread.currentThread().getName(), topic, partition,
                                    msg.get("k").asInt(), sdf.format(date)
                            );
                        }
                        break;
                    case "marker":
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal message type: " + msg.get("type"));
                }
                this.statistics.incDelivered();
                checkFullDelivered();
                break;
            case "summary-stat":
                long amount = Long.parseLong(value);
                this.statistics.setAmount(amount);
                System.out.println("Statistics: " + key + "=" + value);
                checkFullDelivered();
                break;
            default:
                throw new IllegalStateException("Shouldn't be possible to get message on topic " + topic);
        }
    }

    private void checkFullDelivered() {
        if (this.statistics.checkStat())
            System.out.println("Full delivered " + this.statistics.getAmount() + " messages");
    }

    class Statistics {
        private Long amount;
        private AtomicLong delivered = new AtomicLong(0);

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public void incDelivered() {
            this.delivered.getAndIncrement();
        }

        public boolean checkStat() {
            return amount != null && amount > 0 && amount.equals(delivered.get());
        }
    }
}
