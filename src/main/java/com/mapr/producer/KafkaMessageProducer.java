package com.mapr.producer;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface KafkaMessageProducer {
  void sendMessageSync(String topic, String key, String message);

  void close();

  static Properties readProducerProps(String brokers) throws IOException {
    try (InputStream props = Resources.getResource("producer.props").openStream()) {
      Properties properties = new Properties();
      properties.load(props);

      if (brokers != null && !brokers.isEmpty()) {
        properties.put("bootstrap.servers", brokers);
      }

      return properties;
    }
  }
}
