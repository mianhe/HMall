package com.hmall.bff.application;

import com.hmall.bff.infrastructure.BffProperties;
import org.springframework.stereotype.Service;

/** 根据请求路径路由到下游服务。 */
@Service
public class BffRoutingService {

    private final BffProperties props;

    public BffRoutingService(BffProperties props) {
        this.props = props;
    }

    /** 返回下游 base URL，无匹配返回 null。 */
    public String resolveBaseUrl(String path) {
        Downstream ds = resolve(path);
        return ds != null ? ds.baseUrl : null;
    }

    /** 返回下游服务名（用于错误提示），无匹配返回 null。 */
    public String resolveDownstreamName(String path) {
        Downstream ds = resolve(path);
        return ds != null ? ds.name : null;
    }

    private Downstream resolve(String path) {
        if (path.startsWith("/api/categories") || path.startsWith("/api/products") || path.startsWith("/api/files")) {
            return new Downstream("catalog", props.catalog().baseUrl());
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/login")) {
            return new Downstream("user", props.user().baseUrl());
        }
        if (path.startsWith("/api/orders")) {
            return new Downstream("order", props.order().baseUrl());
        }
        if (path.startsWith("/api/inventory")) {
            return new Downstream("inventory", props.inventory().baseUrl());
        }
        return null;
    }

    private record Downstream(String name, String baseUrl) {}
}
