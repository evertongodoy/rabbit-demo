package com.demo.rabbit.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    @RabbitListener(queues = "${rabbitmq.queue1}")
    public void receiveMessage1(String message) {
        log.info("============================================");
        log.info("Mensagem recebida do RabbitMQ - rabbitmq.queue-1 : {}", message);
        log.info("============================================");
    }

    @RabbitListener(queues = "${rabbitmq.queue2}")
    public void receiveMessage2(String message) {
        log.info("============================================");
        log.info("Mensagem recebida do RabbitMQ - rabbitmq.queue-2 : {}", message);
        log.info("============================================");
    }

}