package com.forum.server.config;

import com.forum.common.constant.JwtConstant;
import com.forum.common.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${forum.auth.jwt.secret}")
    private String secret;

    @Value("${forum.auth.jwt.expiration:" + JwtConstant.EXPIRATION + "}")
    private long expiration;

    @PostConstruct
    public void init() {
        JwtUtil.configure(secret, expiration);
    }
}
