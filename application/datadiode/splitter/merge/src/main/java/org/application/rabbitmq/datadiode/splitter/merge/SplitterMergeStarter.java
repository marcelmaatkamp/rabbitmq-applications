package org.application.rabbitmq.datadiode.splitter.merge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class SplitterMergeStarter {
    private static final Logger log = LoggerFactory.getLogger(SplitterMergeStarter.class);

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext configurableApplicationContext = new SpringApplicationBuilder(SplitterMergeStarter.class).web(false).run(args);
    }
}
