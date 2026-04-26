package com.sibanarayan.submission.config;

import com.sibanarayan.submission.events.ProblemEvent;
import com.sibanarayan.submission.events.SubmissionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> factory(Class<T> targetType,String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.sibanarayan.submission.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProblemEvent>
    problemEventFactory() {
        return factory(ProblemEvent.class,"submission-service-problem");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubmissionEvent>
    judgeResultFactory() {
        return factory(SubmissionEvent.class,"submission-service-judge");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubmissionEvent>
    submissionEventFactory() {
        return factory(SubmissionEvent.class, "submission-service-judge");
    }
}
