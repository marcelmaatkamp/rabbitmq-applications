package org.datadiode.service;

import com.thoughtworks.xstream.XStream;
import org.datadiode.model.message.ExchangeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by marcelmaatkamp on 29/10/15.
 */
public class RabbitMQServiceImpl implements RabbitMQService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQServiceImpl.class);

    public static final String X_SHOVELLED = "x-shovelled";
    public static final String SRC_EXCHANGE = "src-exchange";

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    RabbitManagementTemplate rabbitManagementTemplate;

    @Autowired
    XStream xStream;

    @Resource(name="declaredExchanges")
    Map declaredExchanges;

    public ExchangeMessage getExchangeMessage(Message message) {

        String exchangeName = null;

        if(message.getMessageProperties().getHeaders().containsKey(X_SHOVELLED)) {
            ArrayList shovelled_headers = (ArrayList) message.getMessageProperties().getHeaders().get(X_SHOVELLED);
            Map<String, Object> shovelled_headers_map = (Map)shovelled_headers.get(0);
            exchangeName = (String) shovelled_headers_map.get(SRC_EXCHANGE);

            if(log.isDebugEnabled()) {
                log.debug("shovelled from:" + exchangeName);
            }

            if(!declaredExchanges.containsKey(exchangeName)) {
                Exchange exchange = new FanoutExchange(exchangeName);
                declaredExchanges.put(exchangeName, xStream.toXML(exchange));
            }
            return new ExchangeMessage(message, (String) declaredExchanges.get(exchangeName));
        } else {
            exchangeName = message.getMessageProperties().getReceivedExchange();
            if(!declaredExchanges.containsKey(exchangeName)) {
                Exchange exchange = rabbitManagementTemplate.getExchange(message.getMessageProperties().getReceivedExchange());
                declaredExchanges.put(exchangeName, xStream.toXML(exchange));
            }
            return new ExchangeMessage(message, (String) declaredExchanges.get(exchangeName));
        }
    }


    public void sendExchangeMessage(ExchangeMessage exchangeMessage) {
        Exchange exchange = (Exchange) xStream.fromXML(exchangeMessage.getExchangeData());

        if(!declaredExchanges.keySet().contains(exchange)) {
            rabbitAdmin.declareExchange(exchange);
            declaredExchanges.put(exchange.getName(),exchange);
        }

        if(log.isDebugEnabled()) {
            log.debug("exchange("+exchange.getName()+").routing("+exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey()+"): body("+xStream.toXML(exchangeMessage.getMessage())+")");
        }

        // into rabbitmq
        rabbitTemplate.send(
                exchange.getName(),
                exchangeMessage.getMessage().getMessageProperties().getReceivedRoutingKey(),
                exchangeMessage.getMessage()
        );

    }

}
