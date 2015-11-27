package org.application.rabbitmq.splitter.split;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.config.EnableIntegration;

import java.io.IOException;

@SpringBootApplication
@EnableIntegration
@EnableAutoConfiguration
public class SplitterSplitStarter {
    private static final Logger log = LoggerFactory.getLogger(SplitterSplitStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(SplitterSplitStarter.class).web(false).run(args);
    }
}
