/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.sleuth.instrument.batch;

import java.util.Collection;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.lang.Nullable;

public class TraceJobRepository implements JobRepository {

	private final BeanFactory beanFactory;

	private final JobRepository delegate;

	private Tracer tracer;

	public TraceJobRepository(BeanFactory beanFactory, JobRepository delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public boolean isJobInstanceExists(String s, JobParameters jobParameters) {
		return delegate.isJobInstanceExists(s, jobParameters);
	}

	// TODO: TRACE THIS
	@Override
	public JobInstance createJobInstance(String s, JobParameters jobParameters) {
		Span span = tracer().nextSpan().name("jobrepository create")
				.tag("batch.repository.jobName", s)
				.tag("batch.repository.method", "createJobInstance");
		try (Tracer.SpanInScope ws = tracer().withSpan(span.start())) {
			return delegate.createJobInstance(s, jobParameters);
		} finally {
			span.end();
		}
	}

	// TODO: TRACE THIS
	// batch.job.executionId
	@Override
	public JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters, String s) {
		tracer().currentSpan().event("createJobExecution1");
		return delegate.createJobExecution(jobInstance, jobParameters, s);
	}

	// TODO: TRACE THIS
	@Override
	public JobExecution createJobExecution(String s, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		tracer().currentSpan().event("createJobExecution2");
		return delegate.createJobExecution(s, jobParameters);
	}

	// TODO: TRACE THIS
	@Override
	public void update(JobExecution jobExecution) {
		tracer().currentSpan().event("updateJobExecution");
		delegate.update(jobExecution);
	}

	@Override
	public void add(StepExecution stepExecution) {
		delegate.add(stepExecution);
	}

	@Override
	public void addAll(Collection<StepExecution> collection) {
		delegate.addAll(collection);
	}

	// TODO: TRACE THIS
	// batch.job.executionId
	// batch.step.executionId
	@Override
	public void update(StepExecution stepExecution) {
		// ... status?
		tracer().currentSpan().event("update");
		delegate.update(stepExecution);
	}

	// TODO: TRACE THIS
	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		tracer().currentSpan().event("updateStepExecutionContext");
		delegate.updateExecutionContext(stepExecution);
	}

	// TODO: TRACE THIS
	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		tracer().currentSpan().event("updateJobExecutionContext");
		delegate.updateExecutionContext(jobExecution);
	}

	// TODO: TRACE THIS
	@Override
	@Nullable
	public StepExecution getLastStepExecution(JobInstance jobInstance, String s) {
		return delegate.getLastStepExecution(jobInstance, s);
	}

	@Override
	public int getStepExecutionCount(JobInstance jobInstance, String s) {
		return delegate.getStepExecutionCount(jobInstance, s);
	}

	@Override
	@Nullable
	public JobExecution getLastJobExecution(String s, JobParameters jobParameters) {
		return delegate.getLastJobExecution(s, jobParameters);
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

}
