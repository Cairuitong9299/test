package com.example.test;

import com.example.test.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.example.test", "com.bdp.idmapping"})
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Bean
    AuthenticationFilter intiFilter() {return new AuthenticationFilter();}

    public static void main(String[] args) {
        try {
            logger.info("starting");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server Shutting Down...");
            }, "shut-dowm"));
            SpringApplication.run(Application.class, args);
            logger.info("Server Start Success!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
