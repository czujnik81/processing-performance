package com.mobica.poc.processing.monitoring;

import org.javasimon.Counter;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pawel on 23/11/2016.
 */
@Component
public class Monitoring implements MonitoringProperties{

    Map<String, Counter> counters;
    Map<String, Stopwatch> timers;

    public Monitoring() {
        counters = new HashMap<>();
        counters.put("c.consumer", SimonManager.getCounter("c.consumer"));
        counters.put("c.producer", SimonManager.getCounter("c.producer"));
        counters.put("c.bus", SimonManager.getCounter("c.bus"));

        timers = new HashMap<>();
        timers.put("t.consumer", SimonManager.getStopwatch("t.consumer"));
        timers.put("t.producer", SimonManager.getStopwatch("t.producer"));
        timers.put("t.bus", SimonManager.getStopwatch("t.bus"));
    }

    public void increase(String counterName) {
        counters.get(counterName).increase();
    }

    public Split start(String counterName) {
        return timers.get(counterName).start();
    }

    @Override
    public long getConsumerCount() {
        return counters.get("c.consumer").getCounter();
    }

    @Override
    public long getProducerCount() {
        return counters.get("c.producer").getCounter();
    }

    @Override
    public long getConsumerTime() {
        return timers.get("t.consumer").getTotal();
    }

    @Override
    public long getProducerTime() {
        return timers.get("t.producer").getTotal();
    }

    @Override
    public long getBusTime() {
        return timers.get("t.bus").getTotal();
    }

    @Override
    public long getBusCount() {
        return counters.get("c.bus").getCounter();
    }
}