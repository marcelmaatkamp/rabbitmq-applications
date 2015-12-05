package org.application.rabbitmq.datadiode.cutter.merge.configuration.merge;

import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import org.application.rabbitmq.datadiode.cutter.model.SegmentType;
import org.application.rabbitmq.datadiode.model.message.ExchangeMessage;
import org.application.rabbitmq.datadiode.service.RabbitMQService;
import org.application.rabbitmq.datadiode.service.RabbitMQServiceImpl;
import org.application.rabbitmq.datadiode.cutter.model.Segment;
import org.application.rabbitmq.datadiode.cutter.model.SegmentHeader;
import org.application.rabbitmq.datadiode.cutter.util.StreamUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by marcelmaatkamp on 24/11/15.
 */
@Configuration
@EnableScheduling
public class MergeConfiguration implements MessageListener {
    public static final String X_SHOVELLED = "x-shovelled";
    public static final String SRC_EXCHANGE = "src-exchange";
    public static final String SRC_QUEUE = "src-queue";
    private static final Logger log = LoggerFactory.getLogger(MergeConfiguration.class);
    // static Map<SegmentHeader, TreeSet<Segment>> uMessages = new ConcurrentHashMap();

    @Bean
    Map<SegmentHeader, TreeSet<Segment>> uMessages() {
        Map<SegmentHeader, TreeSet<Segment>> uMessages = new ConcurrentHashMap();
        return uMessages;
    }

    @Autowired
    XStream xStream;
    @Autowired
    Environment environment;

    @Value("${application.datadiode.cutter.digest}")
    boolean calculateDigest;

    @Value("${application.datadiode.cutter.digest.name}")
    String digestName;


    @Autowired
    private volatile RabbitTemplate rabbitTemplate;

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

    @Bean
    StreamUtils streamUtils() {
        StreamUtils streamUtils = new StreamUtils();
        return streamUtils;
    }

    @Bean
    RabbitMQService rabbitMQService() {
        RabbitMQService rabbitMQService = new RabbitMQServiceImpl();
        return rabbitMQService;
    }

    @Autowired
    ConnectionFactory connectionFactory;

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }

    @Value(value = "${application.datadiode.cutter.merge.concurrentConsumers}")
    int concurrentConsumers;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(concurrentConsumers);
        return factory;
    }

    // segments

    @Bean
    org.springframework.amqp.core.Exchange segmentHeaderExchange() {
        org.springframework.amqp.core.Exchange exchange = new TopicExchange("segmentHeader");
        rabbitAdmin().declareExchange(exchange);
        return exchange;
    }
    @Bean
    org.springframework.amqp.core.Queue segmentHeaderQueue() {
        org.springframework.amqp.core.Queue queue = new org.springframework.amqp.core.Queue("segmentHeader");
        rabbitAdmin().declareQueue(queue);
        rabbitAdmin().declareBinding(
                new Binding(
                        queue.getName(),
                        Binding.DestinationType.QUEUE,
                        segmentHeaderExchange().getName(),
                        "*", null));
        return queue;
    }

    @Bean
    org.springframework.amqp.core.Exchange segmentExchange() {
        org.springframework.amqp.core.Exchange segmentExchange = new TopicExchange("segment");
        rabbitAdmin().declareExchange(segmentExchange);
        return segmentExchange;
    }
    @Bean
    org.springframework.amqp.core.Queue segmentQueue() {
        org.springframework.amqp.core.Queue queue = new org.springframework.amqp.core.Queue("segment");
        rabbitAdmin().declareQueue(queue);
        rabbitAdmin().declareBinding(
                new Binding(
                        queue.getName(),
                        Binding.DestinationType.QUEUE,
                        segmentExchange().getName(),
                        "*", null));
        return queue;
    }

    boolean parse = true;
    boolean sendToExchanges = false;


    @RabbitListener(

            bindings = @QueueBinding(
                    value = @Queue(value = "${application.datadiode.cutter.queue}", durable = "true"),
                    exchange = @Exchange(value = "${application.datadiode.cutter.exchange}", durable = "true", autoDelete = "false", type = "fanout"))
    )
    public void onMessage(Message message) {

        byte[] segment_or_header = message.getBody();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(segment_or_header);

            byte type = (byte)bis.read();

            if (type == SegmentType.SEGMENT_HEADER.getType()) {
                SegmentHeader segmentHeader = null;
                try {
                    segmentHeader = SegmentHeader.fromByteArray(bis, segment_or_header, calculateDigest);
                    if(sendToExchanges){
                        rabbitTemplate.convertAndSend(segmentHeaderExchange().getName(), segmentHeader.uuid.toString(), segmentHeader);
                    }
                    if(parse) {
                        if (log.isDebugEnabled()) {
                            log.debug("header(" + segmentHeader.uuid + ") of size(" + segmentHeader.blockSize + ") and count(" + segmentHeader.count + ")");
                        }
                        boolean found = false;
                        for (SegmentHeader s : uMessages().keySet()) {
                            if (s.uuid.equals(segmentHeader.uuid)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            uMessages().put(segmentHeader, new TreeSet<Segment>());
                            if (log.isDebugEnabled()) {
                                log.debug("starting message(" + segmentHeader.uuid + ") of size(" + segmentHeader.blockSize + ") and count(" + segmentHeader.count + ")");
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("Exception: ", e);
                } catch (ClassNotFoundException e) {
                    log.error("Exception: ", e);
                }

            } else if(type == SegmentType.SEGMENT.getType()) {
                Segment segment = Segment.fromByteArray(bis, segment_or_header);
                if(sendToExchanges) {
                    rabbitTemplate.convertAndSend(segmentExchange().getName(), segment.uuid.toString(), segment);
                }
                if(parse) {
                    if (log.isTraceEnabled()) {
                        log.trace("segment(" + xStream.toXML(segment) + ")");
                    }

                    for (SegmentHeader segmentHeader : uMessages().keySet()) {
                        if (segmentHeader.uuid.equals(segment.uuid)) {
                            synchronized (segmentHeader) {

                                if (log.isDebugEnabled()) {
                                    log.debug("sh[" + segmentHeader.uuid.toString() + "]: ss[" + segment.uuid.toString() + "] index[" + segment.index + "]: count: " + uMessages().get(segmentHeader).size());
                                }
                                segmentHeader.update = new Date();
                                Set<Segment> messages = uMessages().get(segmentHeader);
                                if (segment != null && messages != null) {
                                    synchronized (uMessages()) {
                                        messages.add(segment);
                                        if (messages.size() == segmentHeader.count + 1) {
                                            ExchangeMessage messageFromStream = StreamUtils.reconstruct(segmentHeader, messages, calculateDigest, digestName);
                                            if (messageFromStream != null) {
                                                if (log.isDebugEnabled()) {
                                                    log.debug("sh[" + segmentHeader.uuid.toString() + "]: ss[" + segment.uuid.toString() + "] index[" + segment.index + "] count(" + messages.size() + "/" + segmentHeader.count + ") sending: " + messageFromStream.getMessage().getMessageProperties().getReceivedExchange());
                                                }
                                                synchronized (segmentHeader) {
                                                    rabbitMQService().sendExchangeMessage(messageFromStream);
                                                    uMessages().remove(segmentHeader);
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    // late arriving message, set already cleaned
                                    // or duplicate segments where original is already constructed

                                    // TODO: into mongo and recontruct segments which arrive earlier than segmentHeaders
                                }
                            }
                        }
                    }
                }
            } else {
                log.error("Unknown type("+type+"), not a segmentHeader or segment");
            }

            bis.close();

        } catch (IOException e) {
            log.error("Exception: ", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Exception: ", e);
        }
    }

    /**
     * Cleaup function
     *
     * @throws MalformedURLException
     */
    @Scheduled(fixedRate = 5000)
    public void cleanup() throws MalformedURLException {
        if (uMessages().keySet().size() > 1) {
            log.warn("concurrent active messages: " + uMessages().keySet().size());
        }

        for (SegmentHeader segmentHeader : uMessages().keySet()) {
            if (segmentHeader.update != null && (new Date().getTime() - segmentHeader.update.getTime()) > 25000) {
                log.info("cleaning up " + segmentHeader.uuid + ", got(" + uMessages().get(segmentHeader).size() + "), missing(" + ((segmentHeader.count + 2) - uMessages().get(segmentHeader).size()) + ")");
                synchronized (uMessages()) {
                    uMessages().remove(segmentHeader);
                }
            }

        }
    }
}
