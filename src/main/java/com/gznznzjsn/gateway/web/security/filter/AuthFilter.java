package com.gznznzjsn.gateway.web.security.filter;

import com.gznznzjsn.gateway.web.dto.AuthEntityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @Autowired
    public AuthFilter(WebClient.Builder loadBalancedWebClientBuilder) {
        super(Config.class);
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new RuntimeException("Auth information is missing!");
            }
            String authHeader = Objects.requireNonNull(
                    exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION)
            ).get(0);
            String[] parts = authHeader.split(" ");

            if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                throw new RuntimeException("Incorrect auth structure!");
            }
            return loadBalancedWebClientBuilder.build()
                    .post()
                    .uri("http://user-service/user-api/v1/auth/validate")
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Mono.just(AuthEntityDto.builder().accessToken(parts[1]).build()), AuthEntityDto.class)
                    .retrieve()
                    .bodyToMono(AuthEntityDto.class)
                    .map(authEntityDto -> {
                        exchange.getRequest()
                                .mutate()
                                .header("userId", String.valueOf(authEntityDto.id()));
                        return exchange;
                    }).flatMap(chain::filter);
        };
    }

    public static class Config {
    }

}
