package com.forum.common.utils;

import com.forum.common.constant.JwtConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtil {

    private static volatile String secretKey;
    private static volatile long expiration = JwtConstant.EXPIRATION;

    /**
     * 将JwtConfig@Value读取的yml数据配置到这里
     * @param secret 特殊签名
     * @param expirationMillis 存活时间
     */
    public static void configure(String secret, long expirationMillis) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than 0");
        }
        secretKey = secret.trim();
        expiration = expirationMillis;
    }
/**
 * 设置方法签名
 */

    private static SecretKey getSigningKey() {
        if (secretKey == null) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param claims 自定义载荷 (如 userId, role)
     * @return token 字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param token token 字符串
     * @return Claims
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 判断 Token 是否过期
     */
    public static boolean isExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
