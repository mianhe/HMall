package com.hmall.payment.infrastructure.scheduling;

import com.hmall.payment.application.PaymentApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.expire-check-enabled", havingValue = "true", matchIfMissing = true)
public class PaymentExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpireScheduler.class);

    private final PaymentApplicationService applicationService;

    public PaymentExpireScheduler(PaymentApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Scheduled(fixedDelayString = "${payment.expire-check-interval-ms:60000}")
    public void runExpireCheck() {
        log.debug("定时超时检测开始");
        applicationService.runExpireCheck();
    }
}
