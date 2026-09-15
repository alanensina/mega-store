package com.alanensina.order.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.orders.topic.name:orders_topic}")
    private String ordersTopicName;

    @Value("${spring.kafka.orders.notification.topic.name:orders_notification}")
    private String notificationTopicName;

    @Bean(name = "ordersTopic")
    public NewTopic ordersTopic(){
        return TopicBuilder.name(ordersTopicName).build();
    }

    @Bean(name = "notificationTopic")
    public NewTopic notificationTopic(){
        return TopicBuilder.name(notificationTopicName).build();
    }
}
