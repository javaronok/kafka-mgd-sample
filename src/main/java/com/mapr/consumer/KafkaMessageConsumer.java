package com.mapr.consumer;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

public interface KafkaMessageConsumer {
  void consume(MessageListener consumeMethod) throws Exception;

  static Properties readConsumerProps(String brokers) throws IOException {
    try (InputStream props = Resources.getResource("consumer.props").openStream()) {
      Properties properties = new Properties();
      properties.load(props);

      if (brokers != null && !brokers.isEmpty()) {
        properties.put("bootstrap.servers", brokers);
      }
      if (properties.getProperty("group.id") == null) {
        properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
      }

      return properties;
    }
  }
}
