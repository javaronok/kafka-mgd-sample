package com.mapr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.tracing.TracingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class PlainTextKafkaConsumer implements KafkaMessageConsumer {

  private final ObjectMapper mapper = new ObjectMapper();

  private final String brokers, topic;

  public PlainTextKafkaConsumer(String brokers, String topic) {
    this.brokers = brokers;
    this.topic = topic;
  }

  @Override
  public void consume(MessageListener consumeMethod) throws Exception {
    Properties properties = KafkaMessageConsumer.readConsumerProps(brokers);

    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
    consumer.subscribe(Collections.singletonList(topic));

    int timeouts = 0;

    TracingService tracer = TracingService.createTracingService();

    while (true) {
      // read records with a short timeout. If we time out, we don't really care.
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
      Thread.yield();
      if (records.count() == 0) {
        timeouts++;
      } else {
        System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
        timeouts = 0;
      }
      for (ConsumerRecord<String, String> record : records) {
        consumeMethod.consumeTopicMessage(record.topic(), record.partition(), record.key(), record.value(), mapper, tracer);
      }
      consumer.commitSync();
    }
  }
}
