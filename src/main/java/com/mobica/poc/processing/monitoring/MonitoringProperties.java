package com.mobica.poc.processing.monitoring;

/**
 * Created by pawel on 23/11/2016.
 */
public interface MonitoringProperties {
    long getConsumerCount();

    long getProducerCount();

    long getConsumerTime();

    long getProducerTime();

    long getBusTime();

    long getBusCount();
}
