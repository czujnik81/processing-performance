package com.mobica.poc.processing.redisson;

import org.redisson.Redisson;
import org.redisson.RedissonNode;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.RedissonNodeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redisson service component
 */
@Component
public class RedissonCmp {

    public static final String EXECUTOR = "myExecutor";
    private RedissonClient redissonClient;
    private RedissonNode redisonNode;

    @Value("${redisson.host}")
    private String redissonHost;

    @Value("${redisson.port}")
    private String redissonPort;

    @PostConstruct
    private void init() {
        Config config = new Config();
        config.useSingleServer().setAddress(redissonHost + ":" + redissonPort).setDatabase(0).setConnectionPoolSize(100);
        this.redissonClient = Redisson.create(config);

        RedissonNodeConfig nodeConfig = new RedissonNodeConfig(config);
        nodeConfig.setExecutorServiceWorkers(Collections.singletonMap(EXECUTOR, 25));
        this.redisonNode = RedissonNode.create(nodeConfig);
        redisonNode.start();
    }

    @PreDestroy
    private void close() {
        redisonNode.shutdown();
        redissonClient.shutdown();
    }

    public void schedule(Callable<String> c) {
        redissonClient.getExecutorService(EXECUTOR).schedule(c, 3, TimeUnit.SECONDS);
    }
}