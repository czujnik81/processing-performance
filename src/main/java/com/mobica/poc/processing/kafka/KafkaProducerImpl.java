package com.mobica.poc.processing.kafka;

import com.mobica.poc.processing.monitoring.Monitoring;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Created by pawel on 25/11/2016.
 */
@Component
public class KafkaProducerImpl implements KafkaProducer {
    @Autowired
    private Monitoring monitoring;

    Logger log = Logger.getLogger(getClass().getName());

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    public void send(String message) {
        log.info("Kafka: " + message);
        Split timer = monitoring.start("t.producer");
        monitoring.increase("c.producer");
        kafkaTemplate.send("kafkatopic", Integer.valueOf(1), message);
        timer.stop();
    }
}