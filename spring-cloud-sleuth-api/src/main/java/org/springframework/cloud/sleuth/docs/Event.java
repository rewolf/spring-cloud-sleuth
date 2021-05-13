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

/**
 * Value representing a notable event happening in time.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.3
 */
public interface Event {

	/**
	 * @return tag value
	 */
	String getValue();

	/**
	 * @return additional tag description
	 */
	default String getDescription() {
		return "";
	}

	/**
	 * Used for static events.
	 * @param value event value
	 * @return tag
	 */
	static Event of(EventValue value) {
		return new ImmutableEvent(value.getValue());
	}

	/**
	 * Used for dynamic events.
	 * @param value event value
	 * @param parameters parameters to be applied to the value
	 * @return tag
	 */
	static Event of(EventValue value, String... parameters) {
		return new ImmutableEvent(String.format(value.getValue(), parameters));
	}

}
