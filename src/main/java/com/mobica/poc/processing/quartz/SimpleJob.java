package com.mobica.poc.processing.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.logging.Logger;

/**
 * Created by pawel on 25/11/2016.
 * Simple Quartz Job
 */
public class SimpleJob implements Job {
    private static final Logger log = Logger.getLogger(SimpleJob.class.getName());
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Consumed Quartz");
    }
}
