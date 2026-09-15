package com.moamoa.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AppConfig {
  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }

  @Bean
  public Executor taskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(200);
    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.setThreadNamePrefix("moamoa-");
    executor.initialize();
    return executor;
  }
}
