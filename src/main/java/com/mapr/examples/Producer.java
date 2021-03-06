package com.mapr.examples;

import com.google.common.io.Resources;
import com.mapr.tracing.TracingService;
import com.mapr.tracing.TracingSpan;
import io.opencensus.trace.TraceId;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * This producer will send a bunch of messages to topic "fast-messages".
 */
public class Producer {
    @Option(name = "-brokers", usage = "Kafka brokers list (delimiter ',')")
    private String brokers = "localhost:9092";

    @Option(name = "-amount", usage = "Amount of messages")
    private Long amount = 1000L;

    @Option(name = "-delay", usage = "Delay between sending")
    private Long delay = 0L;

    public static void main(String[] args) throws IOException {
        Producer producer = new Producer();
        CmdLineParser parser = new CmdLineParser(producer);

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

        producer.run();
    }

    private void run() throws IOException {
        run(brokers, amount, delay);
    }

    public void run(String brokers, long amount, long delay) throws IOException {
        // set up the producer
        KafkaProducer<String, String> producer;
        try (InputStream props = Resources.getResource("producer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);

            if (brokers != null && !brokers.isEmpty()) {
                properties.put("bootstrap.servers", brokers);
            }

            producer = new KafkaProducer<>(properties);
        }

        TracingService tracer = TracingService.createTracingService();

        try {
            for (int i = 0; i < amount; i++) {
                // send lots of messages
                TracingSpan parent = tracer
                        .createSpan("producer")
                        .addLog("Message created")
                        .addTag("message_id", Long.valueOf(i));

                TraceId traceId = parent.traceId();

                TracingSpan formatSpan = tracer.createSpan("format", parent);

                Date t = new Date();
                String message = String.format(
                        "{\"type\":\"test\", \"t\":%d, \"k\":%d, \"traceId\":\"%s\"}",
                        t.getTime(), i, traceId != null ? traceId.toLowerBase16() : "null");

                formatSpan.close();

                try (TracingSpan sendSpan = tracer.createSpan("send", parent)) {
                    producer.send(new ProducerRecord<>("fast-messages", String.valueOf(i), message));
                }

                System.out.println("Sent msg number " + i);

                parent.addLog("Message sent")
                        .close();

                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }
            producer.send(new ProducerRecord<>("summary-stat", "count", String.valueOf(amount)));
        } catch (Throwable throwable) {
            System.out.printf("%s", throwable.getStackTrace());
        } finally {
            producer.close();
        }
    }
}
