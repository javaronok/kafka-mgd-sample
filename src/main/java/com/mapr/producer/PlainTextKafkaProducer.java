package com.mapr.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

public class PlainTextKafkaProducer implements KafkaMessageProducer {
  private final KafkaProducer<String, String> producer;

  public PlainTextKafkaProducer(String brokers) {
    try {
      Properties properties = KafkaMessageProducer.readProducerProps(brokers);
      properties.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

      producer = new KafkaProducer<>(properties);
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
