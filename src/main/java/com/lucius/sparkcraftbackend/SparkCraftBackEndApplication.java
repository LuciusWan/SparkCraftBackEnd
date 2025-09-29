package com.lucius.sparkcraftbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableConfigurationProperties
public class SparkCraftBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparkCraftBackEndApplication.class, args);
    }

}
