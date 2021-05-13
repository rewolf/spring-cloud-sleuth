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

package org.springframework.cloud.sleuth.docs;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;

/**
 * In order to describe your spans via e.g. enums instead of Strings
 * you can use this interface that returns all the characteristics of a span.
 * In Spring Cloud Sleuth we analyze the sources and reuse this
 * information to build a table of known spans, their names, tags and events.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.3
 */
public interface AssertingSpan extends Span {


	DocumentedSpan getDocumentedSpan();

	Span getDelegate();

	@Override
	default AssertingSpan tag(String key, String value) {
		DocumentedSpanAssertions.assertThatKeyIsValid(key, getDocumentedSpan().getTagKeys());
		getDelegate().tag(key, value);
		return this;
	}

	default AssertingSpan tag(TagKey key, String value) {
		DocumentedSpanAssertions.assertThatKeyIsValid(key, getDocumentedSpan().getTagKeys());
		getDelegate().tag(key.getKey(), value);
		return this;
	}

	@Override
	default AssertingSpan event(String value) {
		DocumentedSpanAssertions.assertThatEventIsValid(value, getDocumentedSpan().getEvents());
		getDelegate().event(value);
		return this;
	}

	default AssertingSpan event(EventValue value) {
		DocumentedSpanAssertions.assertThatEventIsValid(value, getDocumentedSpan().getEvents());
		getDelegate().event(value.getValue());
		return this;
	}

	@Override
	default AssertingSpan name(String name) {
		DocumentedSpanAssertions.assertThatNameIsValid(name, getDocumentedSpan().getName());
		getDelegate().name(name);
		return this;
	}

	@Override
	default boolean isNoop() {
		return getDelegate().isNoop();
	}

	@Override
	default TraceContext context() {
		return getDelegate().context();
	}

	@Override
	default AssertingSpan start() {
		getDelegate().start();
		return this;
	}

	@Override
	default AssertingSpan error(Throwable throwable) {
		getDelegate().error(throwable);
		return this;
	}

	@Override
	default void end() {
		getDelegate().end();
	}

	@Override
	default void abandon() {
		getDelegate().abandon();
	}

	@Override
	default AssertingSpan remoteServiceName(String remoteServiceName) {
		getDelegate().remoteServiceName(remoteServiceName);
		return this;
	}

	static AssertingSpan of(DocumentedSpan documentedSpan, Span span) {
		return new ImmutableAssertingSpan(documentedSpan, span);
	}
}