package com.hmall.bff.infrastructure;

import com.hmall.bff.application.BffRoutingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BffProxyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BffProxyFilter.class);

    private static final String[] ALLOWED_ORIGINS = {
        "http://127.0.0.1:5173", "http://127.0.0.1:5174",
        "http://localhost:5173", "http://localhost:5174"
    };

    private final BffRoutingService routingService;
    private final RestTemplate restTemplate = new RestTemplate();

    public BffProxyFilter(BffRoutingService routingService) {
        this.routingService = routingService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && java.util.Arrays.asList(ALLOWED_ORIGINS).contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        addCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String baseUrl = routingService.resolveBaseUrl(path);

        if (baseUrl == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.getWriter().write("{\"message\":\"BFF: no downstream for path " + path + "\"}");
            response.setContentType("application/json");
            return;
        }

        String targetUrl = baseUrl + path + (queryString != null ? "?" + queryString : "");

        try {
            HttpHeaders headers = new HttpHeaders();
            copyForwardHeaders(request, headers);

            byte[] body = readRequestBody(request);
            HttpEntity<byte[]> entity = new HttpEntity<>(body.length > 0 ? body : null, headers);

            ResponseEntity<byte[]> result = restTemplate.exchange(
                URI.create(targetUrl),
                HttpMethod.valueOf(request.getMethod()),
                entity,
                byte[].class
            );

            response.setStatus(result.getStatusCode().value());
            byte[] downstreamBody = result.getBody();
            int bodyLen = downstreamBody != null ? downstreamBody.length : 0;
            result.getHeaders().forEach((name, values) -> {
                String lower = name.toLowerCase();
                if (!isHopByHop(lower) && !"transfer-encoding".equals(lower) && !"content-length".equals(lower)) {
                    values.forEach(v -> response.addHeader(name, v));
                }
            });
            response.setContentLength(bodyLen);
            if (bodyLen > 0) {
                response.getOutputStream().write(downstreamBody);
            }
        } catch (Exception e) {
            log.warn("BFF proxy failed: {} -> {}", request.getMethod() + " " + path, targetUrl, e);
            response.setStatus(HttpStatus.BAD_GATEWAY.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"BFF proxy error: " + e.getMessage() + "\"}");
        }
    }

    private void copyForwardHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (isForwardableHeader(name)) {
                headers.addAll(name, Collections.list(request.getHeaders(name)));
            }
        }
    }

    private static boolean isForwardableHeader(String name) {
        String lower = name.toLowerCase();
        return !isHopByHop(lower)
            && !"host".equals(lower)
            && !"connection".equals(lower)
            && !"content-length".equals(lower);
    }

    private static boolean isHopByHop(String name) {
        return "transfer-encoding".equals(name) || "connection".equals(name);
    }

    private static byte[] readRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
