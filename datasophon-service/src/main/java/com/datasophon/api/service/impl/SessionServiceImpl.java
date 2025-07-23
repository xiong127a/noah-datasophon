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

package com.datasophon.api.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.SessionService;
import com.datasophon.api.utils.HttpUtils;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.SessionEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.SessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service("sessionService")
public class SessionServiceImpl extends ServiceImpl<SessionMapper, SessionEntity> implements SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    // 定义最大会话数量常量
    private static final int MAX_SESSIONS_PER_USER = 1;

    @Autowired
    private SessionMapper sessionMapper;

    /**
     * get user session from request
     *
     * @param request request
     * @return session
     */
    @Override
    public SessionEntity getSession(HttpServletRequest request) {
        // 首先从header获取sessionId
        String sessionId = request.getHeader(Constants.SESSION_ID);

        // 如果header中没有，从cookie获取
        if (StrUtil.isBlank(sessionId)) {
            Cookie cookie = WebUtils.getCookie(request, Constants.SESSION_ID);
            sessionId = Optional.ofNullable(cookie)
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        // 如果sessionId为空，返回null
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }

        String ip = HttpUtils.getClientIpAddress(request);
        logger.debug("get session: {}, ip: {}", sessionId, ip);

        return sessionMapper.selectOneById(sessionId);
    }

    /**
     * create session
     *
     * @param user user
     * @param ip   ip
     * @return session string
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(UserInfoEntity user, String ip) {
        // 查询用户现有会话
        List<SessionEntity> sessionList = sessionMapper.queryByUserId(user.getId());
        Date now = new Date();

        // 会话不为空，表示用户已登录
        if (CollectionUtil.isNotEmpty(sessionList)) {
            // 保持一个用户只有一个会话
            if (sessionList.size() > MAX_SESSIONS_PER_USER) {
                for (int i = MAX_SESSIONS_PER_USER; i < sessionList.size(); i++) {
                    sessionMapper.deleteById(sessionList.get(i).getId());
                }
            }

            SessionEntity session = sessionList.getFirst();

            // 会话未过期，更新最后登录时间并返回
            long sessionAge = now.getTime() - session.getLastLoginTime().getTime();
            if (sessionAge <= Constants.SESSION_TIME_OUT * 1000) {
                session.setLastLoginTime(now);
                this.updateById(session);
                return session.getId();
            } else {
                // 会话过期，删除后创建新会话
                sessionMapper.deleteById(session.getId());
            }
        }

        // 创建新会话
        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setIp(ip);
        session.setUserId(user.getId());
        session.setLastLoginTime(now);

        sessionMapper.insertSession(session);
        return session.getId();
    }

    /**
     * sign out
     * remove ip restrictions
     *
     * @param ip        no use
     * @param loginUser login user
     */
    @Override
    public void signOut(String ip, UserInfoEntity loginUser) {
        try {
            // 查询会话并删除
            SessionEntity session = sessionMapper.queryByUserIdAndIp(loginUser.getId(), ip);
            if (session != null) {
                sessionMapper.deleteById(session.getId());
                logger.debug("User signed out: userId={}, ip={}, sessionId={}",
                        loginUser.getId(), ip, session.getId());
            }
        } catch (Exception e) {
            logger.warn("Multiple sessions found or session not found: userId={}, ip={}, error={}",
                    loginUser.getId(), ip, e.getMessage());
        }
    }
}
