package com.mapr.tracing;

import java.util.function.Supplier;

public interface TracingService<T extends TracingSpan> {

  T createSpan(String name);

  T createSpan(String name, TracingSpan parent);

  T createScopedSpan(String name);

  T createSpanFromRemote(String name, String traceId, String spanId, boolean scoped);

  static <S extends TracingSpan> void withTracing(Runnable r, String name) {
    TracingService<S> tracer = createTracingService();
    S span = tracer.createScopedSpan(name);
    try {
      r.run();
    } finally {
      try {
        span.close();
      } catch (Exception e) {
        System.err.println("Close tracer span error: " + e.getMessage());
      }
    }
  }

  static <S extends TracingSpan, R> R withTracing(Supplier<R> r, String name) {
    TracingService<S> tracer = createTracingService();
    S span = tracer.createScopedSpan(name);
    try {
      return r.get();
    } finally {
      try {
        span.close();
      } catch (Exception e) {
        System.err.println("Close tracer span error: " + e.getMessage());
      }
    }
  }

  static TracingService createTracingService() {
    String serviceName = System.getProperty("serviceName");
    if (enableTracing()) {
      try {
        return OpenCensusTracingService.instance(serviceName);
      } catch (Exception e) {
        System.err.println("Create tracing service error: " + e.getMessage());
        return NoopTracingService.NOOP_SERVICE;
      }
    } else {
      return NoopTracingService.NOOP_SERVICE;
    }
  }

  static boolean enableTracing() {
    String p = System.getProperty("tracing");
    return Boolean.parseBoolean(p);
  }
}
