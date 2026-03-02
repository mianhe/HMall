package com.hmall.order.infrastructure.catalog;

import com.hmall.order.application.port.CatalogServiceQueryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class CatalogServiceQueryAdapter implements CatalogServiceQueryPort {

    private final RestTemplate restTemplate;
    private final String catalogBaseUrl;

    public CatalogServiceQueryAdapter(
            RestTemplate restTemplate,
            @Value("${catalog.base-url:http://localhost:8080}") String catalogBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogBaseUrl = catalogBaseUrl;
    }

    @Override
    public List<AvailableService> findAvailableServices(Long spuId) {
        String url = catalogBaseUrl + "/api/products/" + spuId + "/available-services";
        try {
            ResponseEntity<List<ServiceGroupResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
            );
            List<ServiceGroupResponse> body = response.getBody();
            if (body == null) {
                return List.of();
            }
            return body.stream()
                .filter(g -> g.bindings() != null && "SERVICE".equals(g.productType()))
                .flatMap(g -> g.bindings().stream()
                    .map(b -> new AvailableService(b.serviceSkuId(), g.name(), b.priceCents())))
                .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return List.of();
            }
            throw e;
        }
    }

    private record ServiceGroupResponse(Long serviceSpuId, String name, String productType, List<BindingResponse> bindings) {}
    private record BindingResponse(Long bindingId, Long serviceSkuId, Long priceCents) {}
}
