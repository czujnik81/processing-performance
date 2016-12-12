package com.mobica.poc.processing.springasync;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Created by pawel on 09/12/2016.
 */
@Component
public class SpringScheduler {

    private static final Logger log = Logger.getLogger(SpringScheduler.class.getName());

    @Scheduled(fixedDelay = 5000)
    public void messageScheduled() {
        log.info("SpringScheduler:  Consumed");
    }

    @Async
    public void messageAsync(String message) {
        log.info("SpringAsync: " + message);
    }
}
