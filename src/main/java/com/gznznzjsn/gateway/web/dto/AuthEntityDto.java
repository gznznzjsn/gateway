package com.gznznzjsn.gateway.web.dto;

import lombok.Builder;

@Builder
public record AuthEntityDto(

        Long id,
        String name,
        String email,
        String password,
        String accessToken,
        String refreshToken

) {
}

