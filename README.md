# Performance processing
Proof of Concept project used to check different libraries which could be used for data processing

## Used solutions
Following libraries/solutions are currently covered by this PoC:
1. Kafka
2. Kafka from Spring Async
3. Quartz
4. RabbitMQ with delayed queue
5. Redisson Scheduler
6. Reactor Scheduler

## Configuration
Use following environment variables to configure app:

| Variable name     | Description                                      | Example value |
| ----------------- |:------------------------------------------------:|--------------:|
|REDISSON_HOST      | Redis server host                                | `127.0.0.1`   |
|REDISSON_PORT      | Redis server port                                |     `6379`    |
|KAFKA_HOST         | Kafka broker host                                | `127.0.0.1`   |
|KAFKA_PORT         | Kafka broker port                                |     `9092`    |
|RABBIT_HOST        | RabbitMQ server host                             | `127.0.0.1`   |
|RABBIT_PORT        | RabbitMQ server port                             |     `5672`    |
|PORT               | Server port on which it listen for http requests |     `8080`    |

To run application properly you need to have **Redis**, **Kafka** broker and **RabbitMQ** broker running.

For *RabbitMQ* you should also have **delayed message processing** plugin installed and enabled (details [here](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange))

