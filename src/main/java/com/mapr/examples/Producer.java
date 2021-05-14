package com.mapr.examples;

import com.google.common.io.Resources;
import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.zipkin.ZipkinExporterConfiguration;
import io.opencensus.trace.*;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    private static void initTracing() {
        ZipkinExporterConfiguration cfg = ZipkinExporterConfiguration.builder()
                .setV2Url("http://localhost:9411/api/v2/spans")
                .setServiceName("producer-service")
                .build();
        ZipkinTraceExporter.createAndRegister(cfg);

        TraceConfig traceConfig = Tracing.getTraceConfig();
        TraceParams activeTraceParams = traceConfig.getActiveTraceParams();
        traceConfig.updateActiveTraceParams(
                activeTraceParams.toBuilder().setSampler(Samplers.alwaysSample())
                .setMaxNumberOfAnnotations(10)
                .setMaxNumberOfAttributes(10)
                .build());
    }

    private void run() throws IOException {
        initTracing();
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

        Tracer tracer = Tracing.getTracer();

        try {
            for (int i = 0; i < amount; i++) {
                // send lots of messages
                Span parent = tracer.spanBuilder("producer").startSpan();

                parent.addAnnotation("Message created");
                parent.putAttribute("message_id", AttributeValue.longAttributeValue(i));

                SpanContext traceCtx = parent.getContext();

                Span formatSpan = tracer.spanBuilderWithExplicitParent("format", parent).startSpan();

                Date t = new Date();
                String message = String.format(
                        "{\"type\":\"test\", \"t\":%d, \"k\":%d, \"traceId\":\"%s\"}",
                        t.getTime(), i, traceCtx.getTraceId().toLowerBase16());

                formatSpan.end();

                try (Scope sendScope = tracer.spanBuilderWithExplicitParent("send", parent).startScopedSpan()) {
                    producer.send(new ProducerRecord<>("fast-messages", String.valueOf(i), message));
                }

                System.out.println("Sent msg number " + i);

                parent.addAnnotation("Message sent");
                parent.end();

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
