package com.lucius.sparkcraftbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class SparkCraftBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparkCraftBackEndApplication.class, args);
    }

}
