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

/**
 * In order to describe your spans via e.g. enums instead of Strings
 * you can use this interface that returns all the characteristics of a span.
 * In Spring Cloud Sleuth we analyze the sources and reuse this
 * information to build a table of known spans, their names, tags and events.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.3
 */
public interface DocumentedSpan {

	/**
	 * @return spanName
	 */
	String getName();

	/**
	 * @return tag keys
	 */
	TagKey[] getTagKeys();

	/**
	 * @return events
	 */
	EventValue[] getEvents();

	/**
	 * Asserts on tags, names and allowed events.
	 *
	 * @param span to wrap
	 * @return wrapped span
	 */
	default AssertingSpan wrap(Span span) {
		return AssertingSpan.of(this, span);
	}

}