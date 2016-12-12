package com.mobica.poc.processing.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.logging.Logger;

/**
 * Created by pawel on 09/12/2016.
 */
@RabbitListener(queues = "${rabbit.queue.name}")
public class RabbitReceiver {

    Logger log = Logger.getLogger(getClass().getName());

    @RabbitHandler
    public void receive(String message) {
        log.info("Received Rabbit: " + message);
    }

}