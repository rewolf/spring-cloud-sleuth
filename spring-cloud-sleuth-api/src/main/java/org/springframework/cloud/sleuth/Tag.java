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

package org.springframework.cloud.sleuth;

/**
 * Key/value pair representing a dimension of a span used to classify and drill into measurements.
 * Inspired by Micrometer.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.3
 */
public interface Tag extends Comparable<Tag> {
	/**
	 * @return tag key
	 */
	TagKey getTagKey();

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
	 * Will apply the tag for the given key and value.
	 *
	 * @param span to tag
	 * @return tagged span
	 */
	default Span tag(Span span) {
		return span.tag(getTagKey().getKey(), getValue());
	}

	@Override
	default int compareTo(Tag o) {
		return getTagKey().getKey().compareTo(o.getTagKey().getKey());
	}

	/**
	 * Used for static tag keys. Typically {@link TagKey} will be implemented
	 * by an enum.
	 *
	 * @param key tag key
	 * @param value tag value
	 * @return tag
	 */
	static Tag of(TagKey key, String value) {
		return new ImmutableTag(key, value);
	}

	/**
	 * Used for dynamic tag keys.
	 *
	 * @param key tag key
	 * @param value tag value
	 * @return tag
	 */
	static Tag of(String key, String value, String description) {
		return new ImmutableTag(key, value, description);
	}
}