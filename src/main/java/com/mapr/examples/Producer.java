package com.mapr.examples;

import com.mapr.producer.AvroKafkaProducer;
import com.mapr.producer.KafkaMessageProducer;
import com.mapr.producer.PlainTextKafkaProducer;
import com.mapr.tracing.TracingService;
import com.mapr.tracing.TracingSpan;
import io.opencensus.trace.TraceId;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.Date;

/**
 * This producer will send a bunch of messages to topic "fast-messages".
 */
public class Producer {
    @Option(name = "-brokers", usage = "Kafka brokers list (delimiter ',')")
    private String brokers = "localhost:9092";

    @Option(name = "-registry", usage = "Schema registry URL")
    private String schemaRegistry = "localhost:8081";

    @Option(name = "-amount", usage = "Amount of messages")
    private Long amount = 1000L;

    @Option(name = "-delay", usage = "Delay between sending")
    private Long delay = 0L;

    @Option(name = "-avro", usage = "Avro serialization")
    private Boolean avro = false;

    public static void main(String[] args) {
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

    private void run() {
        run(brokers, schemaRegistry, amount, delay);
    }

    private KafkaMessageProducer createProducer(String brokers, String schemaRegistryUrl) {
        return avro != null && avro
                ? new AvroKafkaProducer(brokers, schemaRegistryUrl)
                : new PlainTextKafkaProducer(brokers);
    }

    public void run(String brokers, String schemaRegistryUrl, long amount, long delay) {
        // set up the producer
        KafkaMessageProducer messageProducer = createProducer(brokers, schemaRegistryUrl);
        PlainTextKafkaProducer statProducer = new PlainTextKafkaProducer(brokers);

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
                    messageProducer.sendMessageSync("fast-messages", String.valueOf(i), message);
                }

                System.out.println("Sent msg number " + i);

                parent.addLog("Message sent").close();

                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }
            statProducer.sendMessageSync("summary-stat", "count", String.valueOf(amount));
        } catch (Throwable throwable) {
            System.out.printf("%s", throwable);
            throwable.printStackTrace();
        } finally {
            messageProducer.close();
            statProducer.close();
        }
    }
}
