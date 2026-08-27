package com.agentplatform.backend.document.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ 交换机、队列和路由配置。
 */
@Configuration
@ConditionalOnProperty(
        name = "app.messaging.enabled",
        havingValue = "true"
)
@Profile("rabbit")
public class RabbitMessagingConfig {

    @Bean
    public TopicExchange documentProcessingExchange(
            @Value("${app.messaging.exchange}") String exchange
    ) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue documentProcessingQueue(
            @Value("${app.messaging.document-processing-queue}") String queue
    ) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding documentProcessingBinding(
            Queue documentProcessingQueue,
            TopicExchange documentProcessingExchange
    ) {
        return BindingBuilder.bind(documentProcessingQueue)
                .to(documentProcessingExchange)
                .with("document.processing");
    }

    /**
     * 文档任务使用 JSON 传输，避免 RabbitMQ 默认 Java 序列化带来的
     * 跨版本兼容和安全风险；消费者和生产者共享稳定的 record 字段。
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new org.springframework.amqp.support.converter
                .Jackson2JsonMessageConverter(objectMapper);
    }
}
