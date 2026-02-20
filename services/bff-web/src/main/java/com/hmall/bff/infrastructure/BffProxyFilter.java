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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/** BFF 透传代理：将 /api/** 请求转发到对应下游服务。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BffProxyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BffProxyFilter.class);

    private final BffRoutingService routingService;
    private final BffProperties properties;
    private final RestTemplate restTemplate;

    public BffProxyFilter(BffRoutingService routingService, BffProperties properties) {
        this.routingService = routingService;
        this.properties = properties;
        BffProperties.Proxy proxy = properties.proxy() != null
            ? properties.proxy()
            : new BffProperties.Proxy(null, 0, 0);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(proxy.connectTimeoutMs());
        factory.setReadTimeout(proxy.readTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        addCorsHeaders(request, response);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        String path = request.getRequestURI();
        String baseUrl = routingService.resolveBaseUrl(path);
        if (baseUrl == null) {
            writeJson(response, HttpStatus.NOT_FOUND.value(), "{\"message\":\"BFF: no route for " + path + "\"}");
            return;
        }

        String queryString = request.getQueryString();
        String targetUrl = baseUrl + path + (queryString != null ? "?" + queryString : "");

        try {
            HttpHeaders headers = new HttpHeaders();
            copyForwardHeaders(request, headers);
            byte[] body = readBody(request);
            HttpEntity<byte[]> entity = new HttpEntity<>(body.length > 0 ? body : null, headers);

            ResponseEntity<byte[]> result = restTemplate.exchange(
                URI.create(targetUrl),
                HttpMethod.valueOf(Objects.requireNonNullElse(request.getMethod(), "GET")),
                entity, byte[].class);

            response.setStatus(result.getStatusCode().value());
            copyResponseHeaders(result, response);
            byte[] downstream = result.getBody();
            if (downstream != null && downstream.length > 0) {
                response.setContentLength(downstream.length);
                response.getOutputStream().write(downstream);
            }
        } catch (RestClientResponseException e) {
            response.setStatus(e.getStatusCode().value());
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String body = e.getResponseBodyAsString();
            response.getWriter().write(body != null && !body.isBlank() ? body : "{\"message\":\"downstream error\"}");
        } catch (Exception e) {
            String downstream = Objects.requireNonNullElse(routingService.resolveDownstreamName(path), "unknown");
            log.warn("BFF proxy failed: {} {} -> {} ({})", request.getMethod(), path, targetUrl, downstream, e);
            writeJson(response, HttpStatus.BAD_GATEWAY.value(),
                "{\"message\":\"" + escapeJson(e.getMessage()) + "\",\"downstream\":\"" + downstream + "\"}");
        }
    }

    // --- helpers ---

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        List<String> allowed = properties.proxy() != null ? properties.proxy().allowedOrigins() : List.of();
        if (origin != null && allowed.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
    }

    private static void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private static void copyForwardHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) return;
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name != null && isForwardable(name)) {
                headers.addAll(name, Collections.list(request.getHeaders(name)));
            }
        }
    }

    private static void copyResponseHeaders(ResponseEntity<byte[]> result, HttpServletResponse response) {
        result.getHeaders().forEach((name, values) -> {
            String lower = name.toLowerCase();
            if (!HOP_BY_HOP.contains(lower) && !"content-length".equals(lower)) {
                values.forEach(v -> response.addHeader(name, v));
            }
        });
    }

    private static final List<String> HOP_BY_HOP = List.of("transfer-encoding", "connection");

    private static boolean isForwardable(String name) {
        String lower = name.toLowerCase();
        return !HOP_BY_HOP.contains(lower) && !"host".equals(lower) && !"content-length".equals(lower);
    }

    private static byte[] readBody(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) { return in.readAllBytes(); }
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
