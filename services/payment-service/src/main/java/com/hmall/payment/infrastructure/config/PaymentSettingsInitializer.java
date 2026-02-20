package com.hmall.payment.infrastructure.config;

import com.hmall.payment.infrastructure.persistence.PaymentSettingEntity;
import com.hmall.payment.infrastructure.persistence.PaymentSettingJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时从数据库加载已持久化的支付设置，覆盖 application.yml 的默认值。
 */
@Component
public class PaymentSettingsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettingsInitializer.class);
    static final String KEY_EXPIRE_MINUTES = "expireMinutes";

    private final PaymentProperties paymentProperties;
    private final PaymentSettingJpaRepository settingRepository;

    public PaymentSettingsInitializer(PaymentProperties paymentProperties,
                                     PaymentSettingJpaRepository settingRepository) {
        this.paymentProperties = paymentProperties;
        this.settingRepository = settingRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        settingRepository.findById(KEY_EXPIRE_MINUTES).ifPresent(entity -> {
            int dbValue = Integer.parseInt(entity.getValue());
            log.info("从数据库加载支付超时设置: expireMinutes={}", dbValue);
            paymentProperties.setExpireMinutes(dbValue);
        });
    }

    /** 保存设置到数据库，供 Controller 调用。 */
    public void persistExpireMinutes(int expireMinutes) {
        settingRepository.save(new PaymentSettingEntity(KEY_EXPIRE_MINUTES, String.valueOf(expireMinutes)));
    }
}
