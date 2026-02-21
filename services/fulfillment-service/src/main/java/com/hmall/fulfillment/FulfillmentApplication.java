package com.hmall.fulfillment;

import com.hmall.fulfillment.infrastructure.kafka.FulfillmentKafkaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FulfillmentKafkaProperties.class)
public class FulfillmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentApplication.class, args);
    }
}
