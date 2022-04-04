package com.mapr.avro;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class JsonMessageAvroSerializer implements Serializer<String> {

  private static final TopicNameStrategy SUBJECT_STRATEGY = new TopicNameStrategy();

  private final Serializer messageSerializer = new KafkaAvroSerializer();

  private SchemaRegistryClient schemaRegistry;

  @Override
  public void configure(Map<String, ?> cfg, boolean isKey) {
    if (isKey)
      throw new IllegalStateException("Not implemented for key");

    if (!cfg.containsKey(SCHEMA_REGISTRY_URL_CONFIG))
      throw new IllegalArgumentException("Parameter " + SCHEMA_REGISTRY_URL_CONFIG + " not found in config");

    this.schemaRegistry = new CachedSchemaRegistryClient(
            Collections.singletonList((String) cfg.get(SCHEMA_REGISTRY_URL_CONFIG)),
            1000
    );

    messageSerializer.configure(cfg, false);
  }

  @Override
  public byte[] serialize(String topic, String data) {
    String subject = SUBJECT_STRATEGY.subjectName(topic, false, null);
    try {
      SchemaMetadata schemaMetadata = schemaRegistry.getLatestSchemaMetadata(subject);
      ParsedSchema parsedSchema = schemaRegistry.getSchemaById(schemaMetadata.getId());

      Object record = AvroSchemaUtils.toObject(data, (AvroSchema) parsedSchema);
      return messageSerializer.serialize(topic, record);
    } catch (IOException | RestClientException e) {
      throw new SerializationException(String.format("Error deserializing json %s for subject %s", data, subject), e);
    }
  }
}
