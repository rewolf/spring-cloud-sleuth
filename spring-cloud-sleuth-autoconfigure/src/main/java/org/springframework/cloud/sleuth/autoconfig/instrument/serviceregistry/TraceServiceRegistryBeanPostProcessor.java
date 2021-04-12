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

package org.springframework.cloud.sleuth.autoconfig.instrument.serviceregistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.sleuth.instrument.serviceregistry.TraceServiceRegistry;

/**
 * Bean post processor that wraps a call to an {@link ServiceRegistry} in a trace
 * representation.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
public class TraceServiceRegistryBeanPostProcessor implements BeanPostProcessor {

	private final BeanFactory beanFactory;

	public TraceServiceRegistryBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ServiceRegistry && !(bean instanceof TraceServiceRegistry)) {
			return new TraceServiceRegistry((ServiceRegistry) bean, this.beanFactory);
		}
		return bean;
	}

}
