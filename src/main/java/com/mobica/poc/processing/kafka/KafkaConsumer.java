package com.mobica.poc.processing.kafka;

/**
 * Created by pawel on 09/12/2016.
 */

import com.mobica.poc.processing.monitoring.Monitoring;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.logging.Logger;

/**
 * Kafka consumer
 */
public class KafkaConsumer {

    @Autowired
    private Monitoring monitoring;

    Logger log = Logger.getLogger(getClass().getName());

    @KafkaListener(id = "lo01", topics = "kafkatopic")
    public void consume(String message) {
        Split timer = monitoring.start("t.consumer");
        monitoring.increase("c.consumer");
        log.info("Consumed: " + message);
        timer.stop();
    }
}
