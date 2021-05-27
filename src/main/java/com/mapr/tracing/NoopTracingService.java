package com.mapr.tracing;

public class NoopTracingService implements TracingService<NoopSpan> {
  static final NoopTracingService NOOP_SERVICE = new NoopTracingService();

  @Override
  public NoopSpan createSpan(String name, TracingSpan parent) {
    return NoopSpan.NOOP_SPAN;
  }

  @Override
  public NoopSpan createSpan(String name) {
    return createSpan(name, null);
  }

  @Override
  public NoopSpan createScopedSpan(String name) {
    return createSpan(name);
  }

  @Override
  public NoopSpan createSpanFromRemote(String name, String traceId, String spanId, boolean scoped) {
    return createSpan(name);
  }
}
