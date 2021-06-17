package org.springframework.cloud.sleuth.instrument.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.sleuth.test.TestSpanHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

public abstract class TraceFunctionAwareWrapperTests {

	@Test
	public void test() {
		ApplicationContext context = new SpringApplicationBuilder(SampleConfiguration.class)
		.run("--logging.level.org.springframework.cloud.function=DEBUG",
				"--spring.main.lazy-initialization=true");

		TestSpanHandler spanHandler = context.getBean(TestSpanHandler.class);
		assertThat(spanHandler.reportedSpans()).isEmpty();
		FunctionCatalog catalog = context.getBean(FunctionCatalog.class);
		FunctionInvocationWrapper function = catalog.lookup("greeter");
		Object result = function.get();
		assertThat(spanHandler.reportedSpans().size()).isEqualTo(1);
	}


	@EnableAutoConfiguration
	public static class SampleConfiguration {

		@Bean
		public Supplier<String> greeter() {
			return () -> "hello";
		}
	}
}
