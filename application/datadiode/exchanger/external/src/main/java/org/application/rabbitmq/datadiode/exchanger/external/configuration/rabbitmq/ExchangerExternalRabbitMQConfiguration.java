package org.application.rabbitmq.datadiode.exchanger.external.configuration.rabbitmq;

import org.application.rabbitmq.datadiode.exchanger.external.listener.ExchangeMessageConverterListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Configuration;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.application.rabbitmq.datadiode.service.RabbitMQServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

@Configuration
@EnableScheduling
public class ExchangerExternalRabbitMQConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ExchangerExternalRabbitMQConfiguration.class);

    @Autowired
    AnnotationConfigApplicationContext context;

    String DATA_DIODE_QUEUENAME_SUFFIX = ".dd";

    List<String> standardExchanges =
            Arrays.asList(
                    //"amq.direct",
                    //"amq.fanout",
                    //"amq.headers",
                    //"amq.match",
                    "amq.rabbitmq.log",
                    "amq.rabbitmq.trace",
                    //"amq.topic",
                    "encrypt",
                    "cut",
                    "cutted",
                    "exchange",
                    "udp");

    @Autowired
    Environment environment;


    @Autowired
    XStream xStream;

    Set<String> queueListeners = new TreeSet<String>();

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    Map<String, String> declaredExchanges() {
        Map<String, String> declaredExchanges = new HashMap<>();
        for (Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            declaredExchanges.put(exchange.getName(), xStream.toXML(exchange));
        }
        return declaredExchanges;
    }

    @Bean
    Exchange exchangeExchange() {
        Exchange exchange = new FanoutExchange(environment.getProperty("application.datadiode.exchange.exchange"));
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }

    @Bean
    Queue exchangeQueue() {
        Queue queue = new Queue(environment.getProperty("application.datadiode.exchange.queue"));
        rabbitAdmin().declareQueue(queue);
        rabbitAdmin().declareBinding(new Binding(queue.getName(), Binding.DestinationType.QUEUE, exchangeExchange().getName(), "", null));
        return queue;
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        return rabbitAdmin;
    }

    @Bean
    RabbitManagementTemplate rabbitManagementTemplate() {
        RabbitManagementTemplate rabbitManagementTemplate = new RabbitManagementTemplate(
                "http://" + environment.getProperty("spring.rabbitmq.host") + ":" + environment.getProperty("spring.rabbitmq.management.port", Integer.class) + "/api/",
                environment.getProperty("spring.rabbitmq.username"),
                environment.getProperty("spring.rabbitmq.password")
        );
        return rabbitManagementTemplate;
    }

    /**
     * Automatic adding listeners
     */
    @Scheduled(fixedDelay = 5000)
    void checkForNewExchanges() {

        for (Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            if (exchange.getName() != null && exchange.getName() != "" && !standardExchanges.contains(exchange.getName())) {
                // log.info("exchange(" + exchange.getName() + "/" + exchange.getType() + ").isDurable(" + exchange.isDurable() + ").isAutoDelete(" + exchange.isAutoDelete() + ").args(" + exchange.getArguments() + ")");

                String queueName = exchange.getName() + DATA_DIODE_QUEUENAME_SUFFIX;
                if (queueName.startsWith("amq.")) {
                    queueName = "_" + queueName;
                }

                Queue bindQueue = null;
                boolean bindingExists = false;

                for (Binding binding : rabbitManagementTemplate().getBindings()) {
                    if (binding.getExchange().equals(exchange.getName())) {

                        if (binding.getDestination().equals(queueName)) {
                            // binding exists, so queue exists
                            bindingExists = true;
                            // log.info("binding exists: binding(" + binding.getExchange() + ").destination(" + binding.getDestination() + ").destinationType(" + binding.getDestinationType() + ").routingKey(" + binding.getRoutingKey() + ").isDestinationQueue(" + binding.isDestinationQueue() + ").args(" + binding.getArguments() + ")");
                        } else {

                            for (Queue queue : rabbitManagementTemplate().getQueues()) {
                                // queue exists, bind
                                if (queue.getName().equals(queueName)) {
                                    log.info("found " + queueName);
                                    bindQueue = queue;
                                }
                            }
                        }
                    }
                }

                if (!bindingExists) {
                    if (bindQueue == null) {
                        log.info("create " + queueName);
                        bindQueue = new Queue(queueName);
                        rabbitAdmin().declareQueue(bindQueue);
                    }
                    log.info("exchange(" + exchange.getName() + ") -> queue(" + bindQueue + ")");
                    BindingBuilder.bind(bindQueue).to(exchange).with("");
                    rabbitAdmin().declareBinding(new Binding(queueName, Binding.DestinationType.QUEUE, exchange.getName(), "", null));
                }

                // queue exists, binding exists, listen!

                if (!queueListeners.contains(queueName)) {
                    log.info("adding listener on " + queueName);
                    SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
                    simpleMessageListenerContainer.setConnectionFactory(rabbitTemplate.getConnectionFactory());
                    simpleMessageListenerContainer.setQueueNames(queueName);
                    simpleMessageListenerContainer.setMessageListener(new MessageListenerAdapter(exchangeMessageConverterListener()));
                    simpleMessageListenerContainer.start();
                    queueListeners.add(queueName);
                }
            }
        }
    }

    @Bean
    ExchangeMessageConverterListener exchangeMessageConverterListener() {
        ExchangeMessageConverterListener exchangeMessageConverterListener = new ExchangeMessageConverterListener();
        return exchangeMessageConverterListener;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

}