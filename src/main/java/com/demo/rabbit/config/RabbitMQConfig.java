package com.demo.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue1}")
    private String queue1;
    @Value("${rabbitmq.queue2}")
    private String queue2;

    @Value("${rabbitmq.routing-key1}")
    private String routingKey1;
    @Value("${rabbitmq.routing-key2}")
    private String routingKey2;

    // Declara a fila
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queue1).build();
    }
    @Bean
    public Queue queue2() {
        return QueueBuilder.durable(queue2).build();
    }

    // Declara o exchange do tipo Direct
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    // Faz o bind da fila ao exchange com a routing key
    @Bean
    public Binding binding1(@Qualifier("queue") Queue queue, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey1);
    }
    @Bean
    public Binding binding2(@Qualifier("queue2") Queue queue2, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue2)
                .to(exchange)
                .with(routingKey2);
    }

    // Conversor de mensagens para JSON
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Template configurado com o conversor JSON
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
