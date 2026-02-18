package com.hmall.payment.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;

public class PaymentSmokeStepDefinitions {

    @假如("Payment 上下文已就绪")
    public void paymentContextReady() {}

    @那么("验收测试应通过")
    public void assertionShouldPass() {}
}
