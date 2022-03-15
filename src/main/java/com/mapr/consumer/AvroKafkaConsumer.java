package com.mapr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.mapr.avro.GenericMessageAvroDeserializer;
import com.mapr.tracing.TracingService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.mapr.avro.GenericMessageAvroSerializer.SCHEMA;

public class AvroKafkaConsumer implements KafkaMessageConsumer {
  private final ObjectMapper mapper = new ObjectMapper();

  private final String brokers, topic;

  public AvroKafkaConsumer(String brokers, String topic) {
    this.brokers = brokers;
    this.topic = topic;
  }

  @Override
  public void consume(MessageListener consumeMethod) throws Exception {
    KafkaConsumer<String, List<GenericRecord>> consumer;
    try {
      File f = new File(Resources.getResource("test-message.avsc").getFile());
      Schema schema = new Schema.Parser().parse(f);

      Properties properties = KafkaMessageConsumer.readConsumerProps(brokers);

      Map<String, Object> cfg = Maps.newHashMap(Maps.fromProperties(properties));
      cfg.put(SCHEMA, schema);

      Deserializer<String> keyDeserializer = new StringDeserializer();
      keyDeserializer.configure(cfg, true);

      Deserializer<List<GenericRecord>> valDeserializer = new GenericMessageAvroDeserializer();
      valDeserializer.configure(cfg, false);

      consumer = new KafkaConsumer<>(cfg, keyDeserializer, valDeserializer);
    } catch (IOException e) {
      throw new RuntimeException("Can't init consumer", e);
    }

    consumer.subscribe(Collections.singletonList(topic));

    int timeouts = 0;

    TracingService tracer = TracingService.createTracingService();

    while (true) {
      // read records with a short timeout. If we time out, we don't really care.
      ConsumerRecords<String, List<GenericRecord>> records = consumer.poll(Duration.ofSeconds(1));
      Thread.yield();
      if (records.count() == 0) {
        timeouts++;
      } else {
        System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
        timeouts = 0;
      }
      for (ConsumerRecord<String, List<GenericRecord>> record : records) {
        String message = record.value().iterator().next().toString();
        consumeMethod.consumeTopicMessage(record.topic(), record.partition(), record.key(), message, mapper, tracer);
      }
      consumer.commitSync();
    }
  }
}
