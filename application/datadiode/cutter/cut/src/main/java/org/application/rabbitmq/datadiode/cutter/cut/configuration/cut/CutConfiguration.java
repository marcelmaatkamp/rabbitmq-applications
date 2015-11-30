package org.application.rabbitmq.datadiode.cutter.cut.configuration.cut;

import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.cutter.cut.configuration.listener.ExchangeMessageConverterListener;
import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.application.rabbitmq.datadiode.service.RabbitMQServiceImpl;
import org.application.rabbitmq.stream.util.StreamUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
@EnableScheduling
public class CutConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CutConfiguration.class);

    String DATA_DIODE_QUEUENAME_SUFFIX = ".dd";

    @Bean
    public JsonMessageConverter jsonMessageConverter() {
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

    @PostConstruct
    void init() {
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
    }


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
                    "udp");


    @Autowired
    XStream xStream;

    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

    @Autowired
    Environment environment;

    @Bean
    MessageDigest messageDigest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest;
    }

    @Bean
    StreamUtils streamUtils() {
        StreamUtils streamUtils = new StreamUtils();
        return streamUtils;
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        return rabbitAdmin;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }
    // TODO: naamgeving in application.properties

    @Bean
    org.springframework.amqp.core.Exchange cutterExchange() {
        org.springframework.amqp.core.Exchange exchange = new FanoutExchange(environment.getProperty("application.datadiode.cutter.cutted.exchange"));
        rabbitAdmin().declareExchange(exchange);
        return exchange;
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


    Set<String> queueListeners = new TreeSet<String>();

    /**
     * Automatic adding listeners
     */
    @Scheduled(fixedDelay = 5000)
    void checkForNewExchanges() {

        for (org.springframework.amqp.core.Exchange exchange : rabbitManagementTemplate().getExchanges()) {
            if (exchange.getName() != null && exchange.getName() != "" && !standardExchanges.contains(exchange.getName())) {
                // log.info("exchange(" + exchange.getName() + "/" + exchange.getType() + ").isDurable(" + exchange.isDurable() + ").isAutoDelete(" + exchange.isAutoDelete() + ").args(" + exchange.getArguments() + ")");

                String queueName = exchange.getName() + DATA_DIODE_QUEUENAME_SUFFIX;
                if (queueName.startsWith("amq.")) {
                    queueName = "_" + queueName;
                }

                org.springframework.amqp.core.Queue bindQueue = null;
                boolean bindingExists = false;

                for (Binding binding : rabbitManagementTemplate().getBindings()) {
                    if (binding.getExchange().equals(exchange.getName())) {

                        if (binding.getDestination().equals(queueName)) {
                            // binding exists, so queue exists
                            bindingExists = true;
                            // log.info("binding exists: binding(" + binding.getExchange() + ").destination(" + binding.getDestination() + ").destinationType(" + binding.getDestinationType() + ").routingKey(" + binding.getRoutingKey() + ").isDestinationQueue(" + binding.isDestinationQueue() + ").args(" + binding.getArguments() + ")");
                        } else {

                            for (org.springframework.amqp.core.Queue queue : rabbitManagementTemplate().getQueues()) {
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
                        bindQueue = new org.springframework.amqp.core.Queue(queueName);
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


/**
    @Scheduled(fixedRate = 25)
    public void sendMessage() throws MalformedURLException {
        int length = 20000;
        rabbitTemplate.send("cut", null, new Message(RandomUtils.nextBytes(length), new MessageProperties()));
    }
*/

}
