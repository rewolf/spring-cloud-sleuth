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

package org.springframework.cloud.sleuth.instrument.annotation;

import org.springframework.cloud.sleuth.docs.EventValue;

enum SleuthAnnotationEvents implements EventValue {

	/**
	 * Annotated before executing a method annotated with @ContinueSpan or @NewSpan.
	 */
	BEFORE {
		@Override
		public String getValue() {
			return "%s.before";
		}
	},

	/**
	 * Annotated after executing a method annotated with @ContinueSpan or @NewSpan.
	 */
	AFTER {
		@Override
		public String getValue() {
			return "%s.after";
		}
	},

	/**
	 * Annotated after throwing an exception from a method annotated with @ContinueSpan or @NewSpan.
	 */
	AFTER_FAILURE {
		@Override
		public String getValue() {
			return "%.afterFailure";
		}
	}

}
