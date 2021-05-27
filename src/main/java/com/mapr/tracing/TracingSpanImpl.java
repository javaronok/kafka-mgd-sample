package com.mapr.tracing;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.TraceId;

public class TracingSpanImpl implements TracingSpan {
  private final io.opencensus.trace.Span span;

  private final io.opencensus.trace.Span old;

  TracingSpanImpl(Span span, boolean scoped) {
    this.span = span;
    if (scoped) {
      old = OpenCensusTracingService.setSpanContext(span);
    } else {
      old = null;
    }
  }

  @Override
  public Span getSpan() {
    return span;
  }

  @Override
  public TracingSpan addTag(String tagName, String tagVal) {
    span.putAttribute(tagName, AttributeValue.stringAttributeValue(tagVal != null ? tagVal : "null"));
    return this;
  }

  @Override
  public TracingSpan addTag(String tagName, Long tagVal) {
    if (tagVal != null)
      span.putAttribute(tagName, AttributeValue.longAttributeValue(tagVal));
    return this;
  }

  @Override
  public TracingSpan addLog(String logDesc) {
    span.addAnnotation(logDesc);
    return this;
  }

  @Override
  public TraceId traceId() {
    return span.getContext().getTraceId();
  }

  @Override
  public void close() {
    try {
      span.end();
    } finally {
      if (old != null) {
        OpenCensusTracingService.setSpanContext(old);
      }
    }
  }
}
