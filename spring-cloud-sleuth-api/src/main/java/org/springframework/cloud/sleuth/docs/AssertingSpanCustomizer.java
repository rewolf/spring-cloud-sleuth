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

import org.springframework.cloud.sleuth.SpanCustomizer;

/**
 * In order to describe your spans via e.g. enums instead of Strings
 * you can use this interface that returns all the characteristics of a span.
 * In Spring Cloud Sleuth we analyze the sources and reuse this
 * information to build a table of known spans, their names, tags and events.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.3
 */
public interface AssertingSpanCustomizer extends SpanCustomizer {

	DocumentedSpan getDocumentedSpan();

	SpanCustomizer getDelegate();

	@Override
	default AssertingSpanCustomizer tag(String key, String value) {
		DocumentedSpanAssertions.assertThatKeyIsValid(key, getDocumentedSpan().getTagKeys());
		getDelegate().tag(key, value);
		return this;
	}

	default AssertingSpanCustomizer tag(TagKey key, String value) {
		DocumentedSpanAssertions.assertThatKeyIsValid(key, getDocumentedSpan().getTagKeys());
		getDelegate().tag(key.getKey(), value);
		return this;
	}

	@Override
	default AssertingSpanCustomizer event(String value) {
		DocumentedSpanAssertions.assertThatEventIsValid(value, getDocumentedSpan().getEvents());
		getDelegate().event(value);
		return this;
	}

	default AssertingSpanCustomizer event(EventValue value) {
		DocumentedSpanAssertions.assertThatEventIsValid(value, getDocumentedSpan().getEvents());
		getDelegate().event(value.getValue());
		return this;
	}

	@Override
	default AssertingSpanCustomizer name(String name) {
		DocumentedSpanAssertions.assertThatNameIsValid(name, getDocumentedSpan().getName());
		getDelegate().name(name);
		return this;
	}

	static AssertingSpanCustomizer of(DocumentedSpan documentedSpan, SpanCustomizer span) {
		return new ImmutableAssertingSpanCustomizer(documentedSpan, span);
	}

}