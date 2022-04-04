package com.mapr.avro;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.Schema;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonMessageAvroDeserializer implements Deserializer<String> {
  private final Deserializer messageDeserializer = new KafkaAvroDeserializer();

  @Override
  public void configure(Map<String, ?> cfg, boolean isKey) {
    if (isKey)
      throw new IllegalStateException("Not implemented for key");

    messageDeserializer.configure(cfg, false);
  }

  @Override
  public String deserialize(String topic, byte[] data) {
    Object record = messageDeserializer.deserialize(topic, data);
    try {
      return new String(AvroSchemaUtils.toJson(record), StandardCharsets.UTF_8);
    } catch (IOException e) {
      Schema schema = AvroSchemaUtils.getSchema(record);
      throw new SerializationException(
              String.format("Error serializing Avro data of schema %s to json", schema), e);
    }
  }

}
