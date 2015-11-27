package org.datadiode.black.configuration.rabbitmq;


import com.thoughtworks.xstream.XStream;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.datadiode.black.listener.GenericMessageUdpSenderListener;
import org.datadiode.configuration.xstream.XStreamConfiguration;
import org.datadiode.service.RabbitMQService;
import org.datadiode.service.RabbitMQServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

/**
 * Created by marcel on 23-09-15.
 */
@Configuration
@EnableScheduling
@Import(XStreamConfiguration.class)
public class RabbitmqConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RabbitmqConfiguration.class);

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
                    "cut");

    @Autowired
    Environment environment;

    int index = 1000;
    int count = 1;
    @Autowired
    XStream xStream;
    Set<String> queueListeners = new TreeSet<String>();

    @Bean
    public MessageConverter jsonMessageConverter() {
        JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();
        jsonMessageConverter.setJsonObjectMapper(objectMapper());
        jsonMessageConverter.setClassMapper(defaultClassMapper());
        return jsonMessageConverter;
    }

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper
                .configure(
                        DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
        jsonObjectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        return jsonObjectMapper;
    }

    @Bean
    public DefaultClassMapper defaultClassMapper() {
        DefaultClassMapper defaultClassMapper = new DefaultClassMapper();
        return defaultClassMapper;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(environment.getProperty("spring.datadiode.rabbitmq.host"));
        connectionFactory.setPort(environment.getProperty("spring.datadiode.rabbitmq.port", Integer.class));
        connectionFactory.setUsername(environment.getProperty("spring.datadiode.rabbitmq.username"));
        connectionFactory.setPassword(environment.getProperty("spring.datadiode.rabbitmq.password"));
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        // rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        return rabbitAdmin;
    }

    @Bean
    RabbitManagementTemplate rabbitManagementTemplate() {
        RabbitManagementTemplate rabbitManagementTemplate = new RabbitManagementTemplate(
                "http://" + environment.getProperty("spring.datadiode.rabbitmq.host") + ":" + environment.getProperty("spring.datadiode.rabbitmq.port.management", Integer.class) + "/api/",
                environment.getProperty("spring.datadiode.rabbitmq.username"),
                environment.getProperty("spring.datadiode.rabbitmq.password")
        );
        return rabbitManagementTemplate;
    }

    @Bean
    Map<String, String> declaredExchanges() {
        Map<String, String> declaredExchanges = new HashMap<>();
        for (Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            declaredExchanges.put(exchange.getName(), xStream.toXML(exchange));
        }
        return declaredExchanges;
    }

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
                    simpleMessageListenerContainer.setConnectionFactory(connectionFactory());
                    simpleMessageListenerContainer.setQueueNames(queueName);
                    simpleMessageListenerContainer.setMessageListener(new MessageListenerAdapter(genericMessageUdpSenderListener()));
                    simpleMessageListenerContainer.start();

                    queueListeners.add(queueName);
                }
            }
        }
    }

    @Bean
    GenericMessageUdpSenderListener genericMessageUdpSenderListener() {
        GenericMessageUdpSenderListener genericMessageUdpSenderListener = new GenericMessageUdpSenderListener();
        genericMessageUdpSenderListener.setCompress(environment.getProperty("application.datadiode.black.udp.compress", Boolean.class));
        return genericMessageUdpSenderListener;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }
}
