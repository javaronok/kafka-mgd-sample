package com.mapr.tracing;

import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.TraceId;

public class NoopSpan implements TracingSpan {
  static final NoopSpan NOOP_SPAN = new NoopSpan();

  @Override
  public Span getSpan() {
    return BlankSpan.INSTANCE;
  }

  @Override
  public TracingSpan addTag(String tagName, String tagVal) {
    return this;
  }

  @Override
  public TracingSpan addTag(String tagName, Long tagVal) {
    return this;
  }

  @Override
  public TracingSpan addLog(String logDesc) {
    return this;
  }

  @Override
  public TraceId traceId() {
    return null;
  }

  @Override
  public void close() {
    // nothing
  }
}
