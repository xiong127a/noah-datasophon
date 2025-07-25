package com.datasophon.common.security;

import java.util.Date;
import java.util.Map;

/**
 * 令牌服务接口
 * 不依赖于Spring Security，提供基础的令牌操作
 */
public interface TokenService {

    /**
     * 创建访问令牌
     *
     * @param subject  令牌主题（通常是用户名）
     * @param userId   用户ID
     * @param claims   其他声明
     * @param validity 有效期
     * @return 令牌字符串
     */
    String createToken(String subject, String userId, Map<String, Object> claims, Date validity);

    /**
     * 创建刷新令牌
     *
     * @param userId 用户ID
     * @return 刷新令牌字符串
     */
    String createRefreshToken(String userId);

    /**
     * 验证令牌有效性
     *
     * @param token 令牌
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 从令牌中获取用户ID
     *
     * @param token 令牌
     * @return 用户ID
     */
    String getUserIdFromToken(String token);

    /**
     * 获取令牌过期时间
     *
     * @param token 令牌
     * @return 过期时间
     */
    Date getExpirationDateFromToken(String token);

    /**
     * 获取令牌主题
     *
     * @param token 令牌
     * @return 主题
     */
    String getSubjectFromToken(String token);

    /**
     * 从令牌中获取所有声明
     *
     * @param token 令牌
     * @return 声明Map
     */
    Map<String, Object> getAllClaimsFromToken(String token);
}