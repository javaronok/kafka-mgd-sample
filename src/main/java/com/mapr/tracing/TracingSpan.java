package com.mapr.tracing;

import io.opencensus.trace.Span;
import io.opencensus.trace.TraceId;

public interface TracingSpan extends AutoCloseable {
  Span getSpan();

  /**
   * Adds tag to span with {@code String} value.
   *
   * @param tagName Tag name.
   * @param tagVal Tag value.
   */
  TracingSpan addTag(String tagName, String tagVal);

  /**
   * Adds tag to span with {@code Long} value.
   *
   * @param tagName Tag name.
   * @param tagVal Tag value.
   */
  TracingSpan addTag(String tagName, Long tagVal);

  /**
   * Logs work to span.
   *
   * @param logDesc Log description.
   */
  TracingSpan addLog(String logDesc);

  /**
   * Returns the trace identifier associated with this {@code SpanContext}
   *
   * @return trace id
   */
  TraceId traceId();
}
