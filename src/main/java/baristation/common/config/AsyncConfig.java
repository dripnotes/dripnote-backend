package baristation.common.config;

import baristation.common.logging.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 활성화 설정
 * @Async 어노테이션을 통한 비동기 메서드 실행을 위해 필수적으로 필요합니다.
 * 또한 ThreadPoolTaskExecutor에 MDC(TaskDecorator)를 설정하여 호출 스레드의 MDC(예: traceId)를
 * 작업 스레드로 전파하도록 합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(50);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("async-worker-");
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.initialize();
		return executor;
	}
}

