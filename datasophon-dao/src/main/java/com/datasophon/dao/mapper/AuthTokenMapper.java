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

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.AuthTokenEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * JWT认证令牌数据访问层
 */
@Mapper
@Repository
public interface AuthTokenMapper extends BaseMapper<AuthTokenEntity> {

    /**
     * 根据用户ID获取有效的令牌列表
     * 
     * @param userId 用户ID
     * @return 有效的令牌列表
     */
    default List<AuthTokenEntity> findValidTokensByUserId(@Param("userId") Integer userId) {
        return QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getUserId).eq(userId)
                .and(AuthTokenEntity::getIsRevoked).eq(false)
                .and(AuthTokenEntity::getExpiresAt).gt(new Date())
                .orderBy(AuthTokenEntity::getIssuedAt, false)
                .list();
    }

    /**
     * 根据令牌查询
     * 
     * @param token JWT令牌
     * @return 令牌实体
     */
    default AuthTokenEntity findByToken(@Param("token") String token) {
        return QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getToken).eq(token)
                .one();
    }

    /**
     * 根据令牌查询（与findByToken相同，用于兼容性）
     */
    default AuthTokenEntity getByToken(@Param("token") String token) {
        return findByToken(token);
    }

    /**
     * 使令牌失效
     * 
     * @param id     令牌ID
     * @param reason 撤销原因
     * @return 是否成功撤销
     */
    default boolean revokeToken(@Param("id") String id, @Param("reason") String reason) {
        return UpdateChain.of(AuthTokenEntity.class)
                .set(AuthTokenEntity::getIsRevoked, true)
                .set(AuthTokenEntity::getRevokedReason, reason)
                .set(AuthTokenEntity::getUpdatedAt, new Date())
                .where(AuthTokenEntity::getId).eq(id)
                .update();
    }

    /**
     * 清理用户旧令牌，保留最新的N个
     *
     * @param userId           用户ID
     * @param maxTokensPerUser 每个用户保留的最大令牌数
     */
    default void cleanupOldTokens(@Param("userId") Integer userId, @Param("maxTokens") int maxTokensPerUser) {
        List<AuthTokenEntity> allTokens = QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getUserId).eq(userId)
                .orderBy(AuthTokenEntity::getIssuedAt, false)
                .list();

        if (allTokens.size() <= maxTokensPerUser) {
            return;
        }

        List<String> tokenIdsToDelete = allTokens.stream()
                .skip(maxTokensPerUser)
                .map(AuthTokenEntity::getId)
                .toList();

        if (tokenIdsToDelete.isEmpty()) {
            return;
        }

        this.deleteBatchByIds(tokenIdsToDelete);
    }

    /**
     * 更新最后访问时间
     * 
     * @param id             令牌ID
     * @param lastAccessTime 最后访问时间
     * @return 更新是否成功
     */
    default boolean updateLastAccessTime(@Param("id") String id, @Param("lastAccessTime") Date lastAccessTime) {
        return UpdateChain.of(AuthTokenEntity.class)
                .set(AuthTokenEntity::getLastAccessTime, lastAccessTime)
                .set(AuthTokenEntity::getUpdatedAt, new Date())
                .where(AuthTokenEntity::getId).eq(id)
                .update();
    }

    /**
     * 更新最后访问时间（与updateLastAccessTime相同，用于兼容性）
     */
    default boolean updateAccessTime(@Param("id") String id, @Param("lastAccessTime") Date lastAccessTime) {
        return updateLastAccessTime(id, lastAccessTime);
    }

    /**
     * 删除过期的令牌
     * 
     * @param cutoffDate 截止日期
     * @return 删除的记录数
     */
    default int deleteExpiredTokens(@Param("cutoffDate") Date cutoffDate) {
        List<String> expiredTokenIds = QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getExpiresAt).lt(cutoffDate)
                .select(AuthTokenEntity::getId)
                .list()
                .stream()
                .map(AuthTokenEntity::getId)
                .toList();

        if (expiredTokenIds.isEmpty()) {
            return 0;
        }

        return this.deleteBatchByIds(expiredTokenIds);
    }

    /**
     * 获取用户有效令牌数量
     * 
     * @param userId 用户ID
     * @return 有效令牌数量
     */
    default int countValidTokensByUserId(@Param("userId") Integer userId) {
        long count = QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getUserId).eq(userId)
                .and(AuthTokenEntity::getIsRevoked).eq(false)
                .and(AuthTokenEntity::getExpiresAt).gt(new Date())
                .count();
        return (int) count;
    }

    /**
     * 删除用户最旧的令牌
     * 
     * @param userId 用户ID
     * @param count  要删除的令牌数量
     * @return 删除的记录数
     */
    default int deleteOldestTokens(@Param("userId") Integer userId, @Param("count") int count) {
        List<String> oldestTokenIds = QueryChain.of(AuthTokenEntity.class)
                .where(AuthTokenEntity::getUserId).eq(userId)
                .and(AuthTokenEntity::getIsRevoked).eq(false)
                .orderBy(AuthTokenEntity::getIssuedAt, true)
                .limit(count)
                .select(AuthTokenEntity::getId)
                .list()
                .stream()
                .map(AuthTokenEntity::getId)
                .toList();

        if (oldestTokenIds.isEmpty()) {
            return 0;
        }

        return this.deleteBatchByIds(oldestTokenIds);
    }
}