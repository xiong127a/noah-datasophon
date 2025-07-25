/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT令牌提供者基础实现类
 * 处理JWT令牌的创建、验证和解析等基本功能
 */
public class JwtTokenProviderBase implements TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProviderBase.class);

    protected static final String AUTHORITIES_KEY = "auth";
    protected static final String USER_ID_KEY = "uid";

    @Value("${jwt.secret:aSingleVeryVerySecretKeyForDatasophonAppSignatureNeeds}")
    protected String secretKeyString;

    @Value("${jwt.expiration:86400000}")
    protected long tokenValidityInMilliseconds; // 默认24小时

    @Value("${jwt.refresh-expiration:2592000000}") // 默认30天
    protected long refreshTokenValidityInMilliseconds;

    protected Key secretKey;

    @PostConstruct
    public void init() {
        try {
            // 使用安全的密钥生成方式，生成适合HS512算法的密钥
            this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            logger.info("成功初始化JWT密钥，使用安全随机生成的HS512密钥");
        } catch (Exception e) {
            logger.error("初始化JWT密钥失败", e);
            throw new SecurityException("无法初始化JWT密钥", e);
        }
    }

    @Override
    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        // 从认证对象中获取用户ID
        Object principal = authentication.getPrincipal();
        String userId = null;
        if (principal instanceof User) {
            userId = ((User) principal).getUsername();
        }

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(USER_ID_KEY, userId)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .filter(auth -> !auth.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.error("JWT令牌已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("不支持的JWT令牌: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("JWT格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("无效的JWT签名: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("无效的JWT令牌: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get(USER_ID_KEY, String.class);
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    @Override
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}