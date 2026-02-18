package com.hmall.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    /** 支付单超时分钟数，未配置时默认 30。 */
    private int expireMinutes = 30;

    /** 模拟支付页 base URL（生成 payUrl 用），默认 http://localhost:8084。 */
    private String mockPayBaseUrl = "http://localhost:8084";

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public String getMockPayBaseUrl() {
        return mockPayBaseUrl;
    }

    public void setMockPayBaseUrl(String mockPayBaseUrl) {
        this.mockPayBaseUrl = mockPayBaseUrl;
    }

    /** Order 服务 base URL；配置后回调成功会 POST 通知 Order 更新订单状态。 */
    private String orderBaseUrl = "http://localhost:8081";

    public String getOrderBaseUrl() {
        return orderBaseUrl;
    }

    public void setOrderBaseUrl(String orderBaseUrl) {
        this.orderBaseUrl = orderBaseUrl;
    }
}
