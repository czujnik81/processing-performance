package com.mobica.poc.processing.kafka;

/**
 * Created by pawel on 25/11/2016.
 */
public interface KafkaProducer {
    public void send(String message);
}
