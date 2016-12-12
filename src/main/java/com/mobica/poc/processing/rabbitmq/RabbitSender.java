package com.mobica.poc.processing.rabbitmq;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class RabbitSender {
    Logger log = Logger.getLogger(getClass().getName());

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private Queue queue;

    public void send(String message) {
        MessageProperties prop = new MessageProperties();
        prop.setDelay(5000);
        prop.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        prop.setContentEncoding("UTF-8");
        log.info("Send Rabbit: " + message);
        this.template.send(queue.getName(), MessageBuilder.withBody(message.getBytes()).andProperties(prop).build());
    }

    @Async
    public void sendAsync(String message) {
        log.info("Send Async Rabbit: " + message);
        this.template.convertAndSend(queue.getName(), message);
    }
}