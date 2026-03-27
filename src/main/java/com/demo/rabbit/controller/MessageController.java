package com.demo.rabbit.controller;

import com.demo.rabbit.messaging.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducer messageProducer;

    @PostMapping("/queue/{numQueue}")
    public ResponseEntity<String> sendMessage(@RequestBody String message, @PathVariable int numQueue) {
        log.info("Requisição recebida na Controller | message={}", message);

        messageProducer.sendMessage(message, numQueue);

        return ResponseEntity.ok("Mensagem enviada ao RabbitMQ com sucesso: " + message);
    }
}
