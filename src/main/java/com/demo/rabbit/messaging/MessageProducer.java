package com.demo.rabbit.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key1}")
    private String routingKey1;
    @Value("${rabbitmq.routing-key2}")
    private String routingKey2;

    public void sendMessage(String message, int numQueue) {
        log.info("Publicando mensagem no RabbitMQ | exchange={} | message={}", exchange, message);

        if(numQueue == 1) {
            rabbitTemplate.convertAndSend(exchange, routingKey1, message);
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey2, message);
        }

        log.info("Mensagem publicada com sucesso.");
    }
}
