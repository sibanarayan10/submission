package com.sibanarayan.submission.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic submissionPendingTopic() {
        return TopicBuilder.name("submission.pending")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic SubmissionResultTopic() {
        return TopicBuilder.name("submission.result")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic judgeResultsTopic() {
        return TopicBuilder.name("judge.results")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic problemEventsTopic() {
        return TopicBuilder.name("problem.events")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventTopic() {
        return TopicBuilder.name("user.event")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
