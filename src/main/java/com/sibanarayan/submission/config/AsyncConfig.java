package com.sibanarayan.submission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("judgeExecutor")
    public Executor judgeExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor(); // Java 21 virtual threads
    }
}