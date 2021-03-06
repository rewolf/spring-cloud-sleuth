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

package org.springframework.cloud.sleuth.test;

import java.util.List;
import java.util.Queue;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;

public interface TestSpanHandler extends Iterable<FinishedSpan> {

	List<FinishedSpan> reportedSpans();

	FinishedSpan takeLocalSpan();

	void clear();

	FinishedSpan takeRemoteSpan(Span.Kind kind);

	FinishedSpan takeRemoteSpanWithError(Span.Kind kind);

	FinishedSpan get(int index);

	void assertAllSpansWereFinishedOrAbandoned(Queue<Span> createdSpans);

}
