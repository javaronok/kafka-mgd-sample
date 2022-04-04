package com.mapr.producer;

import com.google.common.collect.Maps;
import com.mapr.avro.JsonMessageAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class AvroKafkaProducer implements KafkaMessageProducer {

  private final KafkaProducer<String, String> producer;

  public AvroKafkaProducer(String brokers, String schemaUrl) {
    try {
      Properties properties = KafkaMessageProducer.readProducerProps(brokers);
      Map<String, Object> cfg = Maps.newHashMap(Maps.fromProperties(properties));
      cfg.put(SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);

      Serializer<String> keySerializer = new StringSerializer();
      keySerializer.configure(cfg, true);

      Serializer valSerializer = new JsonMessageAvroSerializer();
      valSerializer.configure(cfg, false);

      producer = new KafkaProducer<>(cfg, keySerializer, valSerializer);
    } catch (IOException e) {
      throw new RuntimeException("Can't init producer", e);
    }
  }

  @Override
  public void sendMessageSync(String topic, String key, String message) {
    ProducerRecord<String, String> r = new ProducerRecord<>(topic, key, message);
    try {
      producer.send(r).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Error during sent message", e);
    }
  }

  @Override
  public void close() {
    producer.close();
  }
}
