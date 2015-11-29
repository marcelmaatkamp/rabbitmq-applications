package org.application.rabbitmq.datadiode.exchanger.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class ExchangerExternalStarter {
    private static final Logger log = LoggerFactory.getLogger(ExchangerExternalStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(ExchangerExternalStarter.class).web(false).run(args);
        configurableApplicationContext.start();
    }
}
