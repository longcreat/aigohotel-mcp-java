package com.aigohotel.mcp.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class McpRequestContextFilter implements WebFilter {
    public static final String REQUEST_HEADERS_CONTEXT_KEY = "mcp.request.headers";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        return chain.filter(exchange)
                .contextWrite(Context.of(REQUEST_HEADERS_CONTEXT_KEY, request.getHeaders()));
    }
}
