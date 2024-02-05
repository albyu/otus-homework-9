package ru.boldyrev.otus.config;


import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${application.rabbitmq.notification.accQueueName}")
    String accQueueName;

    @Value("${application.rabbitmq.notification.authQueueName}")
    String authQueueName;


    @Bean
    public Queue accQueue() {
        return new Queue(accQueueName);
    }

    @Bean
    public Queue authQueue() {
        return new Queue(authQueueName);
    }
}

