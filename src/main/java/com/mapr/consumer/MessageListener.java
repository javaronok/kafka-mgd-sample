package com.mapr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.tracing.TracingService;

@FunctionalInterface
public interface MessageListener {
  void consumeTopicMessage(String topic, int partition, String key, String value, ObjectMapper mapper, TracingService tracer) throws Exception;
}
