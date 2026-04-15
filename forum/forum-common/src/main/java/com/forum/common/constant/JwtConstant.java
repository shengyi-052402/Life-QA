package com.forum.common.constant;

/**
 * JWT 相关常量
 */
public class JwtConstant {

    /** JWT 密钥 (生产环境应从配置文件读取) */
    public static final String SECRET_KEY = "forum-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    /** Token 有效期 (毫秒) - 默认24小时 */
    public static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求头名称 */
    public static final String HEADER_NAME = "Authorization";
}
