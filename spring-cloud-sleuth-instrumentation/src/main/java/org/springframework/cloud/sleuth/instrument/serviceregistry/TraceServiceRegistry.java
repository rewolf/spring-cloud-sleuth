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

package org.springframework.cloud.sleuth.instrument.serviceregistry;

import java.util.Locale;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

/**
 * Wrapper around {@link ServiceRegistry}.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
public class TraceServiceRegistry<R extends Registration> implements ServiceRegistry<R> {

	private final ServiceRegistry<R> delegate;

	private final BeanFactory beanFactory;

	private Tracer tracer;

	public TraceServiceRegistry(ServiceRegistry<R> delegate, BeanFactory beanFactory) {
		this.delegate = delegate;
		this.beanFactory = beanFactory;
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

	@Override
	public void register(R registration) {
		delegateWrappedWithSpan(() -> this.delegate.register(registration), "register");
	}

	private void delegateWrappedWithSpan(Runnable runnable, String name) {
		Span span = tracer().spanBuilder().name(name).remoteServiceName(remoteServiceName()).start();
		try (Tracer.SpanInScope ws = tracer().withSpan(span)) {
			runnable.run();
		}
		catch (Throwable ex) {
			span.error(ex);
			throw ex;
		}
		finally {
			span.end();
		}
	}

	private String remoteServiceName() {
		String name = this.delegate.getClass().getName().toLowerCase(Locale.ROOT);
		if (name.contains("eureka")) {
			return "eureka";
		}
		else if (name.contains("consul")) {
			return "consul";
		}
		else if (name.contains("zookeeper")) {
			return "zookeeper";
		}
		return "unknown";
	}

	@Override
	public void deregister(R registration) {
		delegateWrappedWithSpan(() -> this.delegate.deregister(registration), "deregister");
	}

	@Override
	public void close() {
		delegateWrappedWithSpan(this.delegate::close, "close");
	}

	@Override
	public void setStatus(R registration, String status) {
		delegateWrappedWithSpan(() -> this.delegate.setStatus(registration, status), "setStatus");
	}

	@Override
	public <T> T getStatus(R registration) {
		Span span = tracer().spanBuilder().name("getStatus").remoteServiceName(remoteServiceName()).start();
		try (Tracer.SpanInScope ws = tracer().withSpan(span.start())) {
			T status = this.delegate.getStatus(registration);
			span.tag("status", status.toString());
			return status;
		}
		catch (Throwable ex) {
			span.error(ex);
			throw ex;
		}
		finally {
			span.end();
		}
	}

}
