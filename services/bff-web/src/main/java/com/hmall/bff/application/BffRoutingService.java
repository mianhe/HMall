package com.hmall.bff.application;

import com.hmall.bff.infrastructure.BffProperties;
import org.springframework.stereotype.Service;

/**
 * 根据请求路径返回下游 base URL。
 */
@Service
public class BffRoutingService {

    private final BffProperties props;

    public BffRoutingService(BffProperties props) {
        this.props = props;
    }

    public String resolveBaseUrl(String path) {
        if (path.startsWith("/api/categories") || path.startsWith("/api/products") || path.startsWith("/api/files")) {
            return props.catalog().baseUrl();
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/login")) {
            return props.user().baseUrl();
        }
        if (path.startsWith("/api/orders")) {
            return props.order().baseUrl();
        }
        return null;
    }
}
