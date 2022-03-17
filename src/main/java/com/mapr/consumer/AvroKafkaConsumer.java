package com.mapr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mapr.tracing.TracingService;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class AvroKafkaConsumer implements KafkaMessageConsumer {
  private final ObjectMapper mapper = new ObjectMapper();

  private final String brokers, topic, schemaUrl;

  public AvroKafkaConsumer(String brokers, String topic, String schemaUrl) {
    this.brokers = brokers;
    this.topic = topic;
    this.schemaUrl = schemaUrl;
  }

  @Override
  public void consume(MessageListener consumeMethod) throws Exception {
    KafkaConsumer<String, GenericRecord> consumer;
    try {
      Properties properties = KafkaMessageConsumer.readConsumerProps(brokers);

      Map<String, Object> cfg = Maps.newHashMap(Maps.fromProperties(properties));
      cfg.put(SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);

      Deserializer<String> keyDeserializer = new StringDeserializer();
      keyDeserializer.configure(cfg, true);

      Deserializer valDeserializer = new KafkaAvroDeserializer();
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
      ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(1));
      Thread.yield();
      if (records.count() == 0) {
        timeouts++;
      } else {
        System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
        timeouts = 0;
      }
      for (ConsumerRecord<String, GenericRecord> record : records) {
        String message = record.value().toString();
        consumeMethod.consumeTopicMessage(record.topic(), record.partition(), record.key(), message, mapper, tracer);
      }
      consumer.commitSync();
    }
  }
}
