/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.rsocket;

import java.util.HashSet;
import java.util.Iterator;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.frame.FrameType;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.TracingMetadata;
import io.rsocket.metadata.TracingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.RSocketProxy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.ThreadLocalSpan;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.WithThreadLocalSpan;
import org.springframework.cloud.sleuth.internal.EncodingUtils;
import org.springframework.cloud.sleuth.propagation.Propagator;

/**
 * Tracing representation of a {@link RSocketProxy} for the responder.
 *
 * @author Marcin Grzejszczak
 * @author Oleh Dokuka
 * @since 3.1.0
 */
public class TracingResponderRSocketProxy extends RSocketProxy implements WithThreadLocalSpan {

	private static final Log log = LogFactory.getLog(TracingResponderRSocketProxy.class);

	private final Propagator propagator;

	private final Propagator.Getter<ByteBuf> getter;

	private final Tracer tracer;

	private final ThreadLocalSpan threadLocalSpan;

	private final boolean isZipkinPropagationEnabled;

	public TracingResponderRSocketProxy(RSocket source, Propagator propagator, Propagator.Getter<ByteBuf> getter,
			Tracer tracer, boolean isZipkinPropagationEnabled) {
		super(source);
		this.propagator = propagator;
		this.getter = getter;
		this.tracer = tracer;
		this.threadLocalSpan = new ThreadLocalSpan(tracer);
		this.isZipkinPropagationEnabled = isZipkinPropagationEnabled;
	}

	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		// called on Netty EventLoop
		// there can't be trace context in thread local here
		Span handle = consumerSpanBuilder(payload.sliceMetadata(), FrameType.REQUEST_FNF);
		if (log.isDebugEnabled()) {
			log.debug("Created consumer span " + handle);
		}
		final Payload newPayload = PayloadUtils.cleanTracingMetadata(payload, new HashSet<>(propagator.fields()));
		// @formatter:off
		return Mono.fromRunnable(() -> setSpanInScope(handle))
				.contextWrite(context -> context.put(Span.class, handle)
						.put(TraceContext.class, handle.context()))
				.flatMap(o -> super.fireAndForget(newPayload))
					.doOnError(this::finishSpan)
					.doOnSuccess(__ -> finishSpan(null))
					.doOnCancel(() -> finishSpan(null));
		// @formatter:on
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		Span handle = consumerSpanBuilder(payload.sliceMetadata(), FrameType.REQUEST_CHANNEL);
		if (log.isDebugEnabled()) {
			log.debug("Created consumer span " + handle);
		}
		final Payload newPayload = PayloadUtils.cleanTracingMetadata(payload, new HashSet<>(propagator.fields()));
		// @formatter:off
		return Mono.fromRunnable(() -> setSpanInScope(handle))
				.contextWrite(context -> context.put(Span.class, handle)
						.put(TraceContext.class, handle.context()))
				.flatMap(o -> super.requestResponse(newPayload))
				.doOnError(this::finishSpan)
				.doOnSuccess(__ -> finishSpan(null))
				.doOnCancel(() -> finishSpan(null));
		// @formatter:on
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		Span handle = consumerSpanBuilder(payload.sliceMetadata(), FrameType.REQUEST_CHANNEL);
		if (log.isDebugEnabled()) {
			log.debug("Created consumer span " + handle);
		}
		final Payload newPayload = PayloadUtils.cleanTracingMetadata(payload, new HashSet<>(propagator.fields()));
		// @formatter:off
		return Flux.defer(() -> Mono.fromRunnable(() -> setSpanInScope(handle)))
				.contextWrite(context -> context.put(Span.class, handle)
						.put(TraceContext.class, handle.context()))
				.flatMap(o -> super.requestStream(newPayload))
				.doOnError(this::finishSpan)
				.doOnComplete(() -> finishSpan(null))
				.doOnCancel(() -> finishSpan(null));
		// @formatter:on
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
			final Payload firstPayload = firstSignal.get();
			if (firstPayload != null) {
				Span handle = consumerSpanBuilder(firstPayload.sliceMetadata(), FrameType.REQUEST_CHANNEL);
				if (handle == null) {
					return super.requestChannel(flux);
				}
				if (log.isDebugEnabled()) {
					log.debug("Created consumer span " + handle);
				}
				final Payload newPayload = PayloadUtils.cleanTracingMetadata(firstPayload,
						new HashSet<>(propagator.fields()));
				// @formatter:off
				return Flux.defer(() -> Mono.fromRunnable(() -> setSpanInScope(handle)))
						.contextWrite(context -> context.put(Span.class, handle)
								.put(TraceContext.class, handle.context()))
						.flatMap(o -> super.requestChannel(flux.skip(1).startWith(newPayload)))
						.doOnError(this::finishSpan)
						.doOnComplete(() -> finishSpan(null))
						.doOnCancel(() -> finishSpan(null));
				// @formatter:on
			}
			return flux;
		});
	}

	private Span consumerSpanBuilder(ByteBuf headers, FrameType requestType) {
		Span.Builder consumerSpanBuilder = consumerSpanBuilder(headers);
		if (log.isDebugEnabled()) {
			log.debug("Extracted result from headers " + consumerSpanBuilder);
		}
		final ByteBuf extract = CompositeMetadataUtils.extract(headers,
				WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());
		if (extract == null) {
			// TODO: figure it out
			return null;
		}
		final RoutingMetadata routingMetadata = new RoutingMetadata(extract);
		final Iterator<String> iterator = routingMetadata.iterator();
		// Start and finish a consumer span as we will immediately process it.
		consumerSpanBuilder.kind(Span.Kind.CONSUMER).name(requestType.name() + " " + iterator.next()).start();
		return consumerSpanBuilder.start();
	}

	private Span.Builder consumerSpanBuilder(ByteBuf headers) {
		if (this.isZipkinPropagationEnabled) {
			ByteBuf extract = CompositeMetadataUtils.extract(headers,
					WellKnownMimeType.MESSAGE_RSOCKET_TRACING_ZIPKIN.getString());
			if (extract != null) {
				TracingMetadata tracingMetadata = TracingMetadataCodec.decode(extract);
				Span.Builder builder = this.tracer.spanBuilder();
				// TODO: take it from Tracer?, Long to String
				TraceContext.Builder parentBuilder = this.tracer.traceContextBuilder()
						.sampled(tracingMetadata.isSampled()).traceId(EncodingUtils.fromLong(tracingMetadata.traceId()))
						.parentId(EncodingUtils.fromLong(tracingMetadata.parentId()));
				return builder.setParent(parentBuilder.build());
			}
			else {
				return this.propagator.extract(headers, this.getter);
			}
		}
		return this.propagator.extract(headers, this.getter);
	}

	@Override
	public ThreadLocalSpan getThreadLocalSpan() {
		return this.threadLocalSpan;
	}

}
