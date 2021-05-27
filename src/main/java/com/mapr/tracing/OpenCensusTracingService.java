package com.mapr.tracing;

import io.opencensus.exporter.trace.zipkin.ZipkinExporterConfiguration;
import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter;
import io.opencensus.trace.*;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;

import java.util.concurrent.ThreadLocalRandom;

public class OpenCensusTracingService implements TracingService<TracingSpanImpl> {
  private static volatile OpenCensusTracingService instance;

  private static final ThreadLocal<Span> ctx = ThreadLocal.withInitial(() -> BlankSpan.INSTANCE);

  public static OpenCensusTracingService instance(String serviceName) {
    if (instance == null) {
      synchronized(OpenCensusTracingService.class) {
        if (instance == null) instance = new OpenCensusTracingService(serviceName);
      }
    }
    return instance;
  }

  protected OpenCensusTracingService(String serviceName) {
    initTracing(serviceName);
  }

  private static void initTracing(String serviceName) {
    ZipkinExporterConfiguration cfg = ZipkinExporterConfiguration.builder()
            .setV2Url("http://localhost:9411/api/v2/spans")
            .setServiceName(serviceName)
            .build();
    ZipkinTraceExporter.createAndRegister(cfg);

    TraceConfig traceConfig = Tracing.getTraceConfig();
    TraceParams activeTraceParams = traceConfig.getActiveTraceParams();
    traceConfig.updateActiveTraceParams(
            activeTraceParams.toBuilder().setSampler(Samplers.alwaysSample())
                    .setMaxNumberOfAnnotations(10)
                    .setMaxNumberOfAttributes(10)
                    .build());
  }

  public static Span getSpanContext() {
    return ctx.get();
  }

  static Span setSpanContext(Span span) {
    Span old = ctx.get();
    ctx.set(span);
    return old;
  }

  @Override
  public TracingSpanImpl createSpan(String name) {
    return createSpan(name, null);
  }

  @Override
  public TracingSpanImpl createSpan(String name, TracingSpan parent) {
    Tracer tracer = Tracing.getTracer();
    Span span = parent == null
            ? tracer.spanBuilder(name).startSpan()
            : tracer.spanBuilderWithExplicitParent(name, parent.getSpan()).startSpan();
    return new TracingSpanImpl(span, false);
  }

  public TracingSpanImpl createScopedSpan(String name) {
    Tracer tracer = Tracing.getTracer();
    Span parent = getSpanContext();
    Span span = tracer.spanBuilderWithExplicitParent(name, parent).startSpan();
    return new TracingSpanImpl(span, true);
  }

  @Override
  public TracingSpanImpl createSpanFromRemote(String name, String traceId, String spanId, boolean scoped) {
    Tracer tracer = Tracing.getTracer();
    SpanContext remote = SpanContext.create(
            TraceId.fromLowerBase16(traceId),
            spanId != null ? SpanId.fromLowerBase16(spanId) : SpanId.generateRandomId(ThreadLocalRandom.current()),
            tracer.getCurrentSpan().getContext().getTraceOptions(),
            Tracestate.builder().build());
    Span span = tracer.spanBuilderWithRemoteParent(name, remote).startSpan();
    return new TracingSpanImpl(span, scoped);
  }
}
