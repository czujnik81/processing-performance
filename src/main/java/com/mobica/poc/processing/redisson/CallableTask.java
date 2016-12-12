package com.mobica.poc.processing.redisson;

import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RInject;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Callable task used by Redisson
 */
public class CallableTask implements Callable<String> {

    private String message;

    public CallableTask() {
    }

    public CallableTask(String message) {
        this.message = message;
    }

    private static final Logger log = Logger.getLogger(CallableTask.class.getName());

    @RInject
    RedissonClient redisson;

    @Override
    public String call() throws Exception {
        log.info("Call->Redisson: " + message);
        return message;
    }

    public String getMessage() {
        return message;
    }
}