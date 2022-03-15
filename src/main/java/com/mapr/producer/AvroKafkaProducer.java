package com.mapr.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.mapr.avro.GenericMessageAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static com.mapr.avro.GenericMessageAvroSerializer.SCHEMA;

public class AvroKafkaProducer implements KafkaMessageProducer {
  private final ObjectMapper mapper = new ObjectMapper();

  private final KafkaProducer<String, List<GenericRecord>> producer;

  private final Schema schema;

  public AvroKafkaProducer(String brokers) {
    try {
      File f = new File(Resources.getResource("test-message.avsc").getFile());
      schema = new Schema.Parser().parse(f);

      Properties properties = KafkaMessageProducer.readProducerProps(brokers);
      Map<String, Object> cfg = Maps.newHashMap(Maps.fromProperties(properties));
      cfg.put(SCHEMA, schema);

      Serializer<String> keySerializer = new StringSerializer();
      keySerializer.configure(cfg, true);

      Serializer<List<GenericRecord>> valSerializer = new GenericMessageAvroSerializer();
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
            .build();

    ProducerRecord<String, List<GenericRecord>> r = new ProducerRecord<>(topic, key, Lists.newArrayList(record));
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
