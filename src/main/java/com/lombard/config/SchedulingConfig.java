package com.lombard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// включаю задачи по расписанию (@Scheduled)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
