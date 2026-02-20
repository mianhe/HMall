package com.hmall.payment;

import com.hmall.payment.infrastructure.config.PaymentProperties;
import com.hmall.payment.infrastructure.kafka.PaymentKafkaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({PaymentProperties.class, PaymentKafkaProperties.class})
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
