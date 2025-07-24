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

package com.datasophon.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security配置类
 * 配置安全过滤链、认证管理器、密码编码器等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级安全注解（如@PreAuthorize）
@Slf4j
public class SecurityConfig {

        private final JwtTokenProvider tokenProvider;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Autowired
        public SecurityConfig(JwtTokenProvider tokenProvider, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
                this.tokenProvider = tokenProvider;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        }

        /**
         * 配置安全过滤链
         * 
         * @param http HttpSecurity对象
         * @return 配置好的SecurityFilterChain
         * @throws Exception 如果配置出错
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                // 使用 Lambda DSL 风格配置安全规则
                http
                                // 配置CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 禁用CSRF（因为我们使用JWT令牌）
                                .csrf(AbstractHttpConfigurer::disable)

                                // 配置异常处理
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                                // 使用无状态会话
                                .sessionManagement(sessionManagement -> sessionManagement
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 配置头部安全选项
                                .headers(headers -> headers
                                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                                // 配置请求授权规则
                                .authorizeHttpRequests(authorize -> authorize
                                                // 静态资源
                                                .requestMatchers(
                                                                "/static/**",
                                                                "/webjars/**",
                                                                "/ui/**",
                                                                "/*.html",
                                                                "/*.ico",
                                                                "/favicon.ico")
                                                .permitAll()

                                                // 公开API端点
                                                .requestMatchers(
                                                                "/api/login",
                                                                "/api/register",
                                                                "/api/refresh-token")
                                                .permitAll()

                                                // Swagger文档
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // 健康检查和监控端点
                                                .requestMatchers(
                                                                "/actuator/**",
                                                                "/health",
                                                                "/info")
                                                .permitAll()

                                                // 原有免登录接口
                                                .requestMatchers(
                                                                "/",
                                                                "/ssoEnable")
                                                .permitAll()

                                                // OPTIONS请求允许通过
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // 所有其他请求需要认证
                                                .anyRequest().authenticated())

                                // 添加JWT过滤器
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(tokenProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                // 返回构建的过滤链
                return http.build();
        }

        /**
         * 配置CORS策略
         * 
         * @return CORS配置源
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // 使用allowedOriginPatterns代替allowedOrigins解决CORS错误
                configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));
                configuration.setExposedHeaders(Arrays.asList("X-Auth-Token", "Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        /**
         * 配置认证管理器
         * 
         * @param authConfig 认证配置
         * @return 认证管理器
         * @throws Exception 如果配置出错
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        /**
         * 配置密码编码器
         * 
         * @return BCrypt密码编码器
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
