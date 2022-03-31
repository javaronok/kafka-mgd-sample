package com.mapr.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class AvroKafkaProducer implements KafkaMessageProducer {
  private final ObjectMapper mapper = new ObjectMapper();

  private final KafkaProducer<String, GenericRecord> producer;

  private final Schema schema;

  public AvroKafkaProducer(String brokers, String schemaUrl) {
    try {
      File f = new File(Resources.getResource("test-message.avsc").getFile());
      schema = new Schema.Parser().parse(f);

      Properties properties = KafkaMessageProducer.readProducerProps(brokers);
      Map<String, Object> cfg = Maps.newHashMap(Maps.fromProperties(properties));
      cfg.put(SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);

      Serializer<String> keySerializer = new StringSerializer();
      keySerializer.configure(cfg, true);

      Serializer valSerializer = new KafkaAvroSerializer();
      valSerializer.configure(cfg, false);

      producer = new KafkaProducer<>(cfg, keySerializer, valSerializer);
    } catch (IOException e) {
      throw new RuntimeException("Can't init producer", e);
    }
  }

  @Override
  public void sendMessageSync(String topic, String key, String message) {
    final JsonNode node;
    try {
       node = mapper.readTree(message);
    } catch (IOException e) {
      throw new RuntimeException("Error during parse message", e);
    }

    GenericRecord record = new GenericRecordBuilder(schema)
            .set("type", node.get("type").asText())
            .set("t", node.get("t").asLong())
            .set("k", node.get("k").asInt())
            .set("traceId", node.get("traceId").asText())
            .set("message", node.get("message").asText())
            .build();

    ProducerRecord<String, GenericRecord> r = new ProducerRecord<>(topic, key, record);
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
