package com.mobica.poc.processing;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.mobica.poc.processing.kafka.KafkaConsumer;
import com.mobica.poc.processing.kafka.KafkaProducer;
import com.mobica.poc.processing.monitoring.Monitoring;
import com.mobica.poc.processing.monitoring.MonitoringProperties;
import com.mobica.poc.processing.quartz.SimpleJob;
import com.mobica.poc.processing.rabbitmq.RabbitReceiver;
import com.mobica.poc.processing.rabbitmq.RabbitSender;
import com.mobica.poc.processing.redisson.CallableTask;
import com.mobica.poc.processing.redisson.RedissonCmp;
import com.mobica.poc.processing.springasync.SpringScheduler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class ProcessingApplication {

    @Resource
    private Monitoring apiMonitors;

    @Resource
    private SchedulerFactory schedulerFactory;

    @Value("${kafka.host}")
    private String kafkaHost;

    @Value("${kafka.port}")
    private String kafkaPort;

    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.port}")
    private String rabbitPort;

    @Value("${rabbit.queue.name}")
    private String rabbitQueueName;

    @Value("${redisson.host}")
    private String redissonHost;

    @Value("${redisson.port}")
    private String redissonPort;

    public static void main(String[] args) {
        SpringApplication.run(ProcessingApplication.class, args);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigIn() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ProducerFactory<Integer, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost + ":" + kafkaPort);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    @Bean
    public KafkaTemplate<Integer, String> kafkaTemplate() {
        return new KafkaTemplate<Integer, String>(producerFactory());
    }

    @Bean
    KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(10);
        factory.getContainerProperties().setPollTimeout(2000);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> propsMap = new HashMap<>();
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost + ":" + kafkaPort);
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return propsMap;
    }

    @Bean
    public KafkaConsumer listener() {
        return new KafkaConsumer();
    }

    @Bean
    public MBeanServer mbeanServer() {
        MBeanServerFactoryBean mBeanServerFactoryBean = new MBeanServerFactoryBean();
        mBeanServerFactoryBean.setLocateExistingServerIfPossible(true);
        mBeanServerFactoryBean.afterPropertiesSet();
        return mBeanServerFactoryBean.getObject();
    }

    @Bean
    public InterfaceBasedMBeanInfoAssembler assembler() {
        InterfaceBasedMBeanInfoAssembler interfaceBasedMBeanInfoAssembler = new InterfaceBasedMBeanInfoAssembler();
        interfaceBasedMBeanInfoAssembler.setManagedInterfaces(new Class[]{MonitoringProperties.class});
        return interfaceBasedMBeanInfoAssembler;
    }

    @Bean
    public MBeanExporter apiMonitorExporter(InterfaceBasedMBeanInfoAssembler assembler, MBeanServer mbeanServer) {

        MBeanExporter mBeanExporter = new MBeanExporter();
        Map<String, Object> beans = new HashMap<>();

        beans.put("kafaexample:name=KafkaProducer, Type=ApiMonitor", apiMonitors);

        mBeanExporter.setBeans(beans);

        mBeanExporter.setAssembler(assembler);

        mBeanExporter.setServer(mbeanServer);

        mBeanExporter.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);

        return mBeanExporter;
    }


    @Bean
    public SchedulerFactory schedulerFactory() {
        return new StdSchedulerFactory();
    }

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        final Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.start();
        return scheduler;
    }


    /** RabbitMQ */
    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(rabbitHost, Integer.valueOf(rabbitPort));
    }

    @Bean
    public Queue queue() {
        final Queue queue = new Queue(rabbitQueueName);
        queue.setShouldDeclare(true);
        return queue;
    }

    @Bean
    FanoutExchange exchange() {
        FanoutExchange exchange = new FanoutExchange("exchange");
        exchange.setDelayed(true);
        exchange.setShouldDeclare(true);
        return exchange;
    }

    @Bean
    Binding binding(Queue queue, FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }


    @Bean
    public AmqpAdmin amqpAdmin(Binding binding, FanoutExchange exchange, Queue queue) {
        final RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareQueue(queue);
        admin.declareExchange(exchange);
        admin.declareBinding(binding);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setExchange("exchange");
        return rabbitTemplate;
    }



    @Bean
    public RabbitReceiver rabbitReceiver() {
        return new RabbitReceiver();
    }



    /**/

    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean(destroyMethod = "shutdown")
    RedisClient redisClient(ClientResources clientResources) {
        return RedisClient.create(clientResources, RedisURI.create(redissonHost, Integer.valueOf(redissonPort)));
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, String> connection(RedisClient redisClient) {
        return redisClient.connect();
    }
}


/**
 * Main Rest controller
 * used as
 */
@RestController
class EntryPoint {

    Logger log = Logger.getLogger(getClass().getName());

    @Autowired
    KafkaProducer producer;

    @Autowired
    RedissonCmp redissonCmp;

    @Autowired
    Scheduler scheduler;

    @Autowired
    RabbitSender rabbitSender;

    @Autowired
    StatefulRedisConnection redisConnection;

    @Autowired
    SpringScheduler springScheduler;

    @RequestMapping(value = "/message", method = RequestMethod.GET)
    public DeferredResult<String> message(@RequestParam(value = "p", required = false, defaultValue = "s") String processing, @RequestParam("m") String message) {
        final DeferredResult<String> deferredResult = new DeferredResult<>();

        if(null != processing && "a".equals(processing)) {
            sendAsync(message, deferredResult);
        } else if (null != processing && "b".equals(processing)) {
            sendReactor(message);
            deferredResult.setResult("okr");
        } else if (null != processing && "c".equals(processing)) {
            sendRedisson(message);
            deferredResult.setResult("oks");
        } else if (null != processing && "q".equals(processing)) {
            sendQuartz(message);
            deferredResult.setResult("okq");
        } else if (null != processing && "d".equals(processing)) {
            deferredResult.setResult("okd");
        } else if (null != processing && "ra".equals(processing)) {
            sendAsyncRabbit(message);
            deferredResult.setResult("okra");
        } else if (null != processing && "rs".equals(processing)) {
            sendSyncRabbit(message);
            deferredResult.setResult("okrs");
        } else if (null != processing && "re".equals(processing)) {
            sendRedis(message);
            deferredResult.setResult("okre");
        } else if (null != processing && "ss".equals(processing)) {
            sendSpringScheduled(message);
            deferredResult.setResult("okss");
        } else if (null != processing && "sa".equals(processing)) {
            sendSpringAsync(message);
            deferredResult.setResult("oksa");
        } else {
            send(message);
            deferredResult.setResult("ok");
        }

        return deferredResult;
    }

    @Async
    private void sendAsync(String message, DeferredResult<String> result) {
        send(message);
        result.setResult("oka");
    }

    private void send(String message) {
        log.info("Send: " + message);
        producer.send(message);
    }

    private void sendReactor(String message) {
        log.info("Reactor produce: " + message);
        Schedulers.timer().schedule(
                () -> send(message),
                3,
                TimeUnit.SECONDS
        );
    }

    private void sendRedisson(String message) {
        log.info("Request Redisson:" + message);
        redissonCmp.schedule(new CallableTask(message));
    }

    private void sendRedis(String message) {
        final UUID uuid = UUID.randomUUID();

        redisConnection.sync().set(uuid.toString(), message);
        final String resultMessage = (String) redisConnection.sync().get(uuid.toString());

        log.info("Redis: " + message + " => " + resultMessage);
        redisConnection.async().del(uuid.toString());

    }

    private void sendQuartz(String message) {
        final JobDetail job = JobBuilder.newJob(SimpleJob.class)
                .build();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, 3);

        final Trigger trigger = TriggerBuilder.newTrigger()
                .startAt(calendar.getTime())
                //.startNow()
                .build();

        // Tell quartz to schedule the job using our trigger
        try {
            log.info("Run Quartz:" + message);
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private void sendSyncRabbit(String message) {
        rabbitSender.send(message);
    }

    private void sendAsyncRabbit(String message) {
        rabbitSender.sendAsync(message);
    }

    private void sendSpringScheduled(String message) {
        log.info("Run SpringScheduler:" + message);
        springScheduler.messageScheduled();
    }

    private void sendSpringAsync(String message) {
        log.info("Run SpringAsync:" + message);
        springScheduler.messageAsync(message);
    }
}




