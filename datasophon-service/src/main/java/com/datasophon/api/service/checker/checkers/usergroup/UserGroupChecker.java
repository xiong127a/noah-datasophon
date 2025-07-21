package com.datasophon.api.service.checker.checkers.usergroup;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户和组检查器
 * 检查服务配置中runAs字段指定的用户和组是否存在
 */
@Component
public class UserGroupChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(UserGroupChecker.class);
    private static final String SERVICE_DDL_JSON = "service_ddl.json";

    @Autowired
    private CheckerProperties checkerProperties;

    @Autowired
    private String metaBasePath;

    // 保存需要创建的用户和组
    private final List<String> usersToCreate = new ArrayList<>();
    private final List<String> groupsToCreate = new ArrayList<>();

    // 创建定时缓存，默认过期时间5分钟
    private static final TimedCache<String, Object> META_CACHE = CacheUtil.newTimedCache(5 * 60 * 1000);

    // 缓存键常量
    private static final String CACHE_KEY_USERS = "usergroup:users";
    private static final String CACHE_KEY_GROUPS = "usergroup:groups";
    private static final String CACHE_KEY_SERVICE_FILES = "usergroup:service_files";
    private static final String CACHE_KEY_EXPIRY_TIME = "expiry:";

    // 创建缓存，启用时间统计，启用过期监听
    static {
        // 启动定时清理任务，每分钟执行一次
        META_CACHE.schedulePrune(60 * 1000);

        // 添加日志，方便排查问题
        logger.info("初始化用户组检查器缓存，默认过期时间: 5分钟");
    }

    // 缓存默认用户组映射
    private Map<String, String> defaultGroupMappings;

    /**
     * 获取默认用户组映射
     */
    private Map<String, String> getDefaultGroupMappings() {
        if (defaultGroupMappings == null) {
            defaultGroupMappings = checkerProperties.getUserGroup().getDefaultGroupMappings();
        }
        return defaultGroupMappings;
    }

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 用户和组检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 清空之前的检查结果
            usersToCreate.clear();
            groupsToCreate.clear();

            // 新增：存储需要创建home目录的用户列表
            List<String> usersNeedHomeDir = new ArrayList<>();

            boolean checkFailed = false;

            // 收集所有服务配置文件中的用户和组
            Set<String> users = new HashSet<>();
            Set<String> groups = new HashSet<>();

            // 扫描服务配置文件
            cacheLog.info("扫描服务配置文件中...");
            scanServiceConfigurations(users, groups);

            if (users.isEmpty() && groups.isEmpty()) {
                cacheLog.info("未找到需要检查的用户和组配置");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setStyledHtmlMessage(hostInfo, checkItem, true, "用户和组检查通过", new StringBuilder());
                return checkItem;
            }

            cacheLog.info("需要检查的用户: " + String.join(", ", users));
            cacheLog.info("需要检查的组: " + String.join(", ", groups));

            // 检查组是否存在
            cacheLog.info("\n正在检查组是否存在...");
            for (String group : groups) {
                boolean exists = checkGroupExists(hostInfo, group);
                cacheLog.info("组 " + group + ": " + (exists ? "存在" : "不存在"));
                if (!exists) {
                    groupsToCreate.add(group);
                    checkFailed = true;
                }
            }

            // 检查用户是否存在，以及用户home目录是否存在
            cacheLog.info("\n正在检查用户是否存在...");
            for (String user : users) {
                boolean userExists = checkUserExists(hostInfo, user);
                cacheLog.info("用户 " + user + ": " + (userExists ? "存在" : "不存在"));

                if (!userExists) {
                    usersToCreate.add(user);
                    checkFailed = true;
                } else {
                    // 用户存在，检查home目录
                    cacheLog.info("检查用户 " + user + " 的home目录...");
                    boolean homeExists = checkUserHomeExists(hostInfo, user);

                    if (!homeExists) {
                        cacheLog.warn("用户 " + user + " 存在但home目录不存在");
                        usersNeedHomeDir.add(user);
                        checkFailed = true;
                    }
                }
            }

            // 设置检查状态和消息
            if (checkFailed) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder detailsBuilder = createFailDetails(users, groups, usersNeedHomeDir);
                setStyledHtmlMessage(hostInfo, checkItem, false, "用户和组检查未通过", detailsBuilder);
            } else {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                StringBuilder detailsBuilder = createSuccessDetails(users, groups);
                setStyledHtmlMessage(hostInfo, checkItem, true, "用户和组检查通过", detailsBuilder);
            }

            cacheLog.info("==== 用户和组检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            logger.error("用户和组检查时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);

            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "检查过程中发生错误",
                    e.getMessage()));

            setStyledHtmlMessage(hostInfo, checkItem, false, "用户和组检查失败", detailsBuilder);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        // 检查是否配置为自动创建用户和组
        if (!checkerProperties.getUserGroup().isAutoCreate()) {
            cacheLog.info("配置禁用了自动创建用户和组功能，跳过修复");
            return false;
        }

        cacheLog.info("==== 开始修复用户和组 ====");
        cacheLog.info("主机: " + hostInfo.getIp());

        boolean allFixed = true;

        // 首先创建组
        if (!groupsToCreate.isEmpty()) {
            cacheLog.info("\n创建缺少的组...");
            for (String group : groupsToCreate) {
                cacheLog.info("创建组: " + group);
                boolean success = createGroup(hostInfo, group);
                if (success) {
                    cacheLog.info("组 " + group + " 创建成功");
                } else {
                    cacheLog.error("组 " + group + " 创建失败");
                    allFixed = false;
                }
            }
        }

        // 然后创建用户
        if (!usersToCreate.isEmpty()) {
            cacheLog.info("\n创建缺少的用户...");
            for (String user : usersToCreate) {
                // 为用户找到对应的组
                String group = findGroupForUser(hostInfo, user);
                cacheLog.info("创建用户: " + user + " (组: " + group + ")");
                boolean success = createUser(hostInfo, user, group);
                if (success) {
                    cacheLog.info("用户 " + user + " 创建成功");
                } else {
                    cacheLog.error("用户 " + user + " 创建失败");
                    allFixed = false;
                }
            }
        }

        // 修复缺失的home目录
        List<String> usersNeedHomeDir = getUsersWithoutHomeDir(hostInfo);
        if (!usersNeedHomeDir.isEmpty()) {
            cacheLog.info("\n创建缺少的home目录...");
            for (String user : usersNeedHomeDir) {
                cacheLog.info("为用户 " + user + " 创建home目录");
                boolean success = createHomeDirectory(hostInfo, user);
                if (success) {
                    cacheLog.info("用户 " + user + " 的home目录创建成功");
                } else {
                    cacheLog.error("用户 " + user + " 的home目录创建失败");
                    allFixed = false;
                }
            }
        }

        cacheLog.info("==== 用户和组修复结束 ====");
        return allFixed;
    }

    /**
     * 获取所有存在但没有home目录的用户
     */
    private List<String> getUsersWithoutHomeDir(HostInfo hostInfo) {
        List<String> usersNeedHomeDir = new ArrayList<>();
        try {
            // 从服务配置中获取所有需要检查的用户
            Set<String> users = new HashSet<>();
            Set<String> groups = new HashSet<>();
            scanServiceConfigurations(users, groups);

            // 检查每个用户的home目录
            for (String user : users) {
                // 只检查已存在的用户
                if (checkUserExists(hostInfo, user)) {
                    boolean homeExists = checkUserHomeExists(hostInfo, user);
                    if (!homeExists) {
                        usersNeedHomeDir.add(user);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取缺少home目录的用户列表时发生错误", e);
            cacheLog.error("获取缺少home目录的用户列表时发生错误: " + e.getMessage());
        }
        return usersNeedHomeDir;
    }

    /**
     * 为用户创建home目录
     */
    private boolean createHomeDirectory(HostInfo hostInfo, String username) {
        try {
            // 获取用户的home目录路径
            String getHomeCommand = "getent passwd " + username + " | cut -d: -f6";
            CommandResult homePathResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    getHomeCommand);

            if (!homePathResult.isSuccess() || StringUtils.isBlank(homePathResult.getOutput().trim())) {
                cacheLog.error("无法获取用户 " + username + " 的home目录路径");
                return false;
            }

            String homePath = homePathResult.getOutput().trim();

            // 创建home目录
            String createHomeCommand = "mkdir -p " + homePath + " && chown " + username + ": " + homePath;
            CommandResult createResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    createHomeCommand);

            if (!createResult.isSuccess()) {
                cacheLog.error("创建 " + username + " 的home目录失败: " + createResult.getErrorOrOutput());
                return false;
            }

            // 验证home目录是否已成功创建
            return checkUserHomeExists(hostInfo, username);
        } catch (Exception e) {
            logger.error("为用户 " + username + " 创建home目录时发生错误", e);
            cacheLog.error("为用户 " + username + " 创建home目录时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 扫描服务配置文件，提取所有runAs字段中的用户和组
     */
    private void scanServiceConfigurations(Set<String> users, Set<String> groups) {
        try {
            // 获取配置的版本，如DDP-1.2.1
            String versionDir = checkerProperties.getMeta().getVersions();

            // 构建缓存键，基于版本和元数据路径
            String cacheKeyPrefix = metaBasePath + ":" + versionDir + ":";
            String cacheUserKey = cacheKeyPrefix + CACHE_KEY_USERS;
            String cacheGroupKey = cacheKeyPrefix + CACHE_KEY_GROUPS;
            String cacheExpiryKey = CACHE_KEY_EXPIRY_TIME + cacheKeyPrefix;

            // 尝试从缓存获取用户和组信息
            @SuppressWarnings("unchecked")
            Set<String> cachedUsers = (Set<String>) META_CACHE.get(cacheUserKey);
            @SuppressWarnings("unchecked")
            Set<String> cachedGroups = (Set<String>) META_CACHE.get(cacheGroupKey);

            // 调试信息，检查缓存状态
            logger.debug("检查缓存状态 - 用户缓存: {}, 组缓存: {}",
                    cachedUsers != null ? "存在" : "不存在",
                    cachedGroups != null ? "存在" : "不存在");

            // 获取过期时间信息
            Long expiryTime = (Long) META_CACHE.get(cacheExpiryKey);
            logger.debug("缓存过期时间键: {}, 值: {}", cacheExpiryKey, expiryTime);

            if (cachedUsers != null && cachedGroups != null) {
                // 计算剩余有效期（秒）
                long currentTime = System.currentTimeMillis();
                long remainingSeconds = 0;

                if (expiryTime != null) {
                    logger.debug("当前时间: {}, 过期时间: {}, 差值(ms): {}",
                            currentTime, expiryTime, (expiryTime - currentTime));
                    remainingSeconds = Math.max(0, (expiryTime - currentTime) / 1000);
                } else {
                    logger.debug("未找到缓存项的过期时间信息");
                }

                // 缓存命中，直接使用缓存数据
                if (remainingSeconds > 0) {
                    cacheLog.info("使用缓存的用户和组配置 (剩余有效期: %s分%s秒)",
                            remainingSeconds / 60, remainingSeconds % 60);
                } else {
                    cacheLog.info("使用缓存的用户和组配置 (即将过期)");
                    // 过期的缓存项应该被及时移除，但没有被移除，说明定时任务可能没有正常工作
                    // 手动移除这些过期的缓存项
                    if (expiryTime != null && expiryTime < currentTime) {
                        logger.debug("手动移除过期的缓存项");
                        META_CACHE.remove(cacheUserKey);
                        META_CACHE.remove(cacheGroupKey);
                        META_CACHE.remove(cacheExpiryKey);

                        // 重新扫描
                        cacheLog.info("缓存已过期，重新扫描元数据目录");
                        scanServiceConfigurations(users, groups);
                        return;
                    }
                }
                users.addAll(cachedUsers);
                groups.addAll(cachedGroups);
                return;
            }

            cacheLog.info("缓存未命中或已过期，重新扫描元数据目录");

            // 构建完整的搜索路径
            String searchDir = metaBasePath + File.separator + versionDir;

            cacheLog.info("扫描元数据目录: " + searchDir);

            // 检查路径是否为URI格式
            if (metaBasePath.startsWith("file:") || metaBasePath.startsWith("classpath:")) {
                try {
                    // 尝试使用Spring Resource机制加载资源
                    org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(
                            searchDir);
                    if (resource.exists()) {
                        try {
                            searchDir = resource.getFile().getAbsolutePath();
                        } catch (Exception e) {
                            // 无法转换为文件路径，可能是JAR包内资源
                            cacheLog.warn("无法获取资源文件系统路径: " + e.getMessage());
                            cacheLog.info("将使用默认用户组映射配置进行检查");
                            addDefaultUserGroupMappings(users, groups);
                            return;
                        }
                    } else {
                        cacheLog.warn("元数据目录资源不存在: " + searchDir);
                        cacheLog.info("将使用默认用户组映射配置进行检查");
                        addDefaultUserGroupMappings(users, groups);
                        return;
                    }
                } catch (Exception e) {
                    cacheLog.warn("处理元数据目录资源时出错: " + e.getMessage());
                    cacheLog.info("将使用默认用户组映射配置进行检查");
                    addDefaultUserGroupMappings(users, groups);
                    return;
                }
            }

            File dir = new File(searchDir);
            if (!dir.exists() || !dir.isDirectory()) {
                cacheLog.warn("元数据目录不存在: " + searchDir);

                // 检查IDEA开发环境的特殊路径
                boolean foundInIdea = checkIdeaPaths(versionDir, users, groups);
                if (foundInIdea) {
                    // 缓存结果
                    META_CACHE.put(cacheUserKey, new HashSet<>(users));
                    META_CACHE.put(cacheGroupKey, new HashSet<>(groups));
                    return;
                }

                cacheLog.info("将使用默认用户组映射配置进行检查");
                addDefaultUserGroupMappings(users, groups);
                return;
            }

            // 递归查找所有service_ddl.json文件
            List<File> serviceDdlFiles = new ArrayList<>();

            // 定义文件列表的缓存键
            String cacheFilesKey = cacheKeyPrefix + CACHE_KEY_SERVICE_FILES;
            String cacheFilesExpiryKey = CACHE_KEY_EXPIRY_TIME + cacheFilesKey;

            // 尝试从缓存中获取服务配置文件列表
            @SuppressWarnings("unchecked")
            List<String> cachedFilePaths = (List<String>) META_CACHE.get(cacheFilesKey);
            if (cachedFilePaths != null) {
                // 获取文件列表缓存的过期时间
                Long filesExpiryTime = (Long) META_CACHE.get(cacheFilesExpiryKey);

                // 计算剩余有效期（秒）
                long currentTime = System.currentTimeMillis();
                long remainingSeconds = 0;
                if (filesExpiryTime != null) {
                    logger.debug("文件列表缓存 - 当前时间: {}, 过期时间: {}, 差值(ms): {}",
                            currentTime, filesExpiryTime, (filesExpiryTime - currentTime));
                    remainingSeconds = Math.max(0, (filesExpiryTime - currentTime) / 1000);
                } else {
                    logger.debug("未找到文件列表缓存的过期时间信息");
                }

                if (remainingSeconds > 0) {
                    cacheLog.info("使用缓存的服务配置文件列表 (共 {} 个文件, 剩余有效期: {}分{}秒)",
                            cachedFilePaths.size(), remainingSeconds / 60, remainingSeconds % 60);
                } else {
                    cacheLog.info("使用缓存的服务配置文件列表 (共 {} 个文件, 即将过期)", cachedFilePaths.size());

                    // 手动移除过期的缓存项
                    if (filesExpiryTime != null && filesExpiryTime < currentTime) {
                        logger.debug("手动移除过期的文件列表缓存");
                        META_CACHE.remove(cacheFilesKey);
                        META_CACHE.remove(cacheFilesExpiryKey);

                        // 重新扫描文件系统
                        cacheLog.info("文件列表缓存已过期，重新扫描文件系统");
                        findServiceDdlFiles(dir, serviceDdlFiles);

                        // 缓存新的文件列表
                        List<String> filePaths = new ArrayList<>(serviceDdlFiles.size());
                        for (File file : serviceDdlFiles) {
                            filePaths.add(file.getAbsolutePath());
                        }

                        // 设置新的缓存和过期时间
                        long expiryTimeValue = System.currentTimeMillis() + (5 * 60 * 1000);
                        META_CACHE.put(cacheFilesKey, filePaths);
                        META_CACHE.put(cacheFilesExpiryKey, expiryTimeValue);

                        cacheLog.info("已重新缓存 {} 个服务配置文件路径", filePaths.size());
                        return;
                    }
                }

                // 使用缓存中的文件路径
                for (String path : cachedFilePaths) {
                    File file = new File(path);
                    if (file.exists() && file.isFile()) {
                        serviceDdlFiles.add(file);
                    }
                }
            } else {
                // 缓存未命中，执行文件系统扫描
                cacheLog.info("扫描文件系统查找服务配置文件...");
                findServiceDdlFiles(dir, serviceDdlFiles);

                // 缓存文件路径列表
                List<String> filePaths = new ArrayList<>(serviceDdlFiles.size());
                for (File file : serviceDdlFiles) {
                    filePaths.add(file.getAbsolutePath());
                }

                // 设置新的缓存项
                long expiryTimeValue = System.currentTimeMillis() + (5 * 60 * 1000);
                META_CACHE.put(cacheFilesKey, filePaths);
                META_CACHE.put(cacheFilesExpiryKey, expiryTimeValue);

                // 验证缓存是否设置成功
                Long setExpiry = (Long) META_CACHE.get(cacheFilesExpiryKey);
                logger.debug("文件列表缓存 - 验证过期时间设置: {}", setExpiry);
            }

            cacheLog.info("找到 " + serviceDdlFiles.size() + " 个服务配置文件");

            // 如果没有找到配置文件，使用默认配置
            if (serviceDdlFiles.isEmpty()) {
                cacheLog.info("未找到服务配置文件，将使用默认用户组映射配置进行检查");
                addDefaultUserGroupMappings(users, groups);
                return;
            }

            // 解析每个文件
            for (File file : serviceDdlFiles) {
                parseServiceDdlFile(file, users, groups);
            }

            // 如果解析后没有找到任何用户和组，使用默认配置
            if (users.isEmpty() && groups.isEmpty()) {
                cacheLog.info("配置文件中未找到用户和组配置，将使用默认用户组映射配置进行检查");
                addDefaultUserGroupMappings(users, groups);
            } else {
                // 计算过期时间点（当前时间 + 5分钟）
                long currentTime = System.currentTimeMillis();
                long expiryTimeValue = currentTime + (5 * 60 * 1000);

                logger.debug("设置缓存过期时间: 当前时间: {}, 过期时间: {}", currentTime, expiryTimeValue);

                // 缓存扫描结果
                META_CACHE.put(cacheUserKey, new HashSet<>(users));
                META_CACHE.put(cacheGroupKey, new HashSet<>(groups));
                META_CACHE.put(cacheExpiryKey, expiryTimeValue);

                // 验证缓存是否设置成功
                Long setExpiry = (Long) META_CACHE.get(cacheExpiryKey);
                logger.debug("验证过期时间设置: {}", setExpiry);

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                cacheLog.info("已缓存扫描结果，有效期5分钟 (到期时间: {})",
                        sdf.format(new Date(expiryTimeValue)));
            }
        } catch (Exception e) {
            logger.error("扫描服务配置文件时发生错误", e);
            cacheLog.error("扫描服务配置文件时发生错误: " + e.getMessage());
            // 发生错误时也使用默认配置
            cacheLog.info("由于发生错误，将使用默认用户组映射配置进行检查");
            addDefaultUserGroupMappings(users, groups);
        }
    }

    /**
     * 检查IDEA开发环境中可能的路径位置
     * 
     * @return 是否找到并处理了文件
     */
    private boolean checkIdeaPaths(String versionDir, Set<String> users, Set<String> groups) {
        // 尝试几个IDEA开发环境中可能的路径
        String projectRoot = System.getProperty("user.dir");

        // 如果当前在子模块中，尝试找到项目根目录
        if (projectRoot.contains("datasophon-") || projectRoot.contains("noah-bigdata-platform")) {
            File parentDir = new File(projectRoot).getParentFile();
            if (parentDir != null && parentDir.exists()) {
                projectRoot = parentDir.getAbsolutePath();
            }
        }

        String[] possiblePaths = {
                projectRoot + "/datasophon-api/src/main/resources/meta/" + versionDir,
                projectRoot + "/datasophon-service/src/main/resources/meta/" + versionDir,
                projectRoot + "/src/main/resources/meta/" + versionDir,
                projectRoot + "/conf/meta/" + versionDir,
                projectRoot + "/meta/" + versionDir,
                "E:/project-code/noah-bigdata-platform/datasophon-api/src/main/resources/meta/" + versionDir,
                "E:/project-code/noah-bigdata-platform/datasophon-service/src/main/resources/meta/" + versionDir
        };

        for (String path : possiblePaths) {
            cacheLog.info("尝试在IDEA环境中查找: " + path);
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                cacheLog.info("在IDEA环境中找到元数据目录: " + path);

                // 递归查找所有service_ddl.json文件
                List<File> serviceDdlFiles = new ArrayList<>();
                findServiceDdlFiles(dir, serviceDdlFiles);

                cacheLog.info("找到 " + serviceDdlFiles.size() + " 个服务配置文件");

                if (!serviceDdlFiles.isEmpty()) {
                    // 解析每个文件
                    for (File file : serviceDdlFiles) {
                        parseServiceDdlFile(file, users, groups);
                    }

                    // 如果解析后没有找到任何用户和组，使用默认配置
                    if (users.isEmpty() && groups.isEmpty()) {
                        cacheLog.info("配置文件中未找到用户和组配置，将使用默认配置");
                        addDefaultUserGroupMappings(users, groups);
                    } else {
                        // 为IDEA环境设置缓存
                        String ideaCachePrefix = "idea:" + path + ":";
                        String ideaUserKey = ideaCachePrefix + CACHE_KEY_USERS;
                        String ideaGroupKey = ideaCachePrefix + CACHE_KEY_GROUPS;
                        String ideaExpiryKey = CACHE_KEY_EXPIRY_TIME + ideaCachePrefix;

                        // 设置缓存
                        long expiryTimeValue = System.currentTimeMillis() + (5 * 60 * 1000);
                        META_CACHE.put(ideaUserKey, new HashSet<>(users));
                        META_CACHE.put(ideaGroupKey, new HashSet<>(groups));
                        META_CACHE.put(ideaExpiryKey, expiryTimeValue);

                        cacheLog.info("已为IDEA环境路径缓存用户组配置, 有效期5分钟");
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 添加默认的用户组映射配置
     */
    private void addDefaultUserGroupMappings(Set<String> users, Set<String> groups) {
        Map<String, String> mappings = getDefaultGroupMappings();
        if (mappings != null && !mappings.isEmpty()) {
            // 添加所有默认用户
            users.addAll(mappings.keySet());
            // 添加所有默认组
            groups.addAll(new HashSet<>(mappings.values()));

            cacheLog.info("已添加默认用户: " + String.join(", ", mappings.keySet()));
            cacheLog.info("已添加默认组: " + String.join(", ", new HashSet<>(mappings.values())));
        } else {
            cacheLog.warn("未配置默认用户组映射");
        }
    }

    /**
     * 递归查找所有service_ddl.json文件
     */
    private void findServiceDdlFiles(File dir, List<File> result) {
        if (dir.isDirectory()) {
            // 检查目录结构是否有变化（使用目录修改时间作为标记）
            String cacheKey = "dirmtime:" + dir.getAbsolutePath();
            Long cachedMtime = (Long) META_CACHE.get(cacheKey);
            long currentMtime = dir.lastModified();

            if (cachedMtime != null && cachedMtime != currentMtime) {
                // 目录修改时间变化，清除相关缓存
                logger.info("检测到目录结构变化，清除相关缓存: {}", dir.getAbsolutePath());
                clearRelatedCache(dir);
            }

            // 更新目录修改时间缓存
            META_CACHE.put(cacheKey, currentMtime);

            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        findServiceDdlFiles(file, result);
                    } else if (file.getName().equals(SERVICE_DDL_JSON)) {
                        result.add(file);
                    }
                }
            }
        }
    }

    /**
     * 解析服务配置文件，提取runAs字段
     */
    private void parseServiceDdlFile(File file, Set<String> users, Set<String> groups) {
        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            // 计算文件内容的哈希值用于检测变化
            String cacheKey = "filehash:" + file.getAbsolutePath();
            String contentHash = cn.hutool.crypto.SecureUtil.md5(content);
            String cachedHash = (String) META_CACHE.get(cacheKey);

            // 检查文件内容是否有变化
            if (cachedHash != null && !cachedHash.equals(contentHash)) {
                logger.info("检测到文件内容变化，清除相关缓存: {}", file.getAbsolutePath());
                // 只清除相关缓存，保留其他缓存
                clearRelatedCache(file.getParentFile());
            }

            // 更新缓存中的文件哈希值
            META_CACHE.put(cacheKey, contentHash);

            JSONObject json = JSON.parseObject(content);

            // 检查roles数组
            if (json.containsKey("roles")) {
                JSONArray roles = json.getJSONArray("roles");
                for (int i = 0; i < roles.size(); i++) {
                    JSONObject role = roles.getJSONObject(i);
                    if (role.containsKey("runAs")) {
                        JSONObject runAs = role.getJSONObject("runAs");
                        if (runAs.containsKey("user")) {
                            String user = runAs.getString("user");
                            if (StringUtils.isNotBlank(user)) {
                                users.add(user);
                            }
                        }
                        if (runAs.containsKey("group")) {
                            String group = runAs.getString("group");
                            if (StringUtils.isNotBlank(group)) {
                                groups.add(group);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("解析服务配置文件时发生错误: " + file.getAbsolutePath(), e);
            cacheLog.error("解析文件 " + file.getAbsolutePath() + " 时发生错误: " + e.getMessage());
        }
    }

    /**
     * 清除与指定目录相关的缓存
     * 
     * @param directory 目录
     */
    private void clearRelatedCache(File directory) {
        if (directory == null) {
            return;
        }

        String dirPath = directory.getAbsolutePath();

        // 查找该目录相关的所有缓存键并清除
        Set<String> keysToRemove = new HashSet<>();

        // 获取所有缓存键
        for (String key : META_CACHE.keySet()) {
            if (key.contains(dirPath) ||
                    (key.startsWith(metaBasePath) &&
                            (key.endsWith(CACHE_KEY_USERS) ||
                                    key.endsWith(CACHE_KEY_GROUPS) ||
                                    key.endsWith(CACHE_KEY_SERVICE_FILES)))) {
                keysToRemove.add(key);
            }
        }

        // 删除匹配的缓存项
        for (String key : keysToRemove) {
            META_CACHE.remove(key);
            logger.debug("已清除相关缓存: {}", key);
        }
    }

    /**
     * 检查用户是否存在
     */
    private boolean checkUserExists(HostInfo hostInfo, String username) throws InterruptedException {
        String command = "id -u " + username + " >/dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);
        return result.isSuccess() && "EXISTS".equals(result.getOutput().trim());
    }

    /**
     * 检查用户的home目录是否存在
     */
    private boolean checkUserHomeExists(HostInfo hostInfo, String username) throws InterruptedException {
        // 先获取用户的home目录路径
        String getHomeCommand = "getent passwd " + username + " | cut -d: -f6";
        CommandResult homePathResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                getHomeCommand);

        if (!homePathResult.isSuccess() || StringUtils.isBlank(homePathResult.getOutput().trim())) {
            cacheLog.warn("无法获取用户 " + username + " 的home目录路径");
            return false;
        }

        String homePath = homePathResult.getOutput().trim();
        cacheLog.info("用户 " + username + " 的home目录路径: " + homePath);

        // 检查home目录是否存在
        String checkHomeCommand = "[ -d \"" + homePath + "\" ] && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult homeExistsResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                checkHomeCommand);

        boolean exists = homeExistsResult.isSuccess() && "EXISTS".equals(homeExistsResult.getOutput().trim());
        cacheLog.info("用户 " + username + " 的home目录" + (exists ? "存在" : "不存在"));
        return exists;
    }

    /**
     * 检查组是否存在
     */
    private boolean checkGroupExists(HostInfo hostInfo, String groupname) throws InterruptedException {
        String command = "getent group " + groupname + " >/dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);
        return result.isSuccess() && "EXISTS".equals(result.getOutput().trim());
    }

    /**
     * 创建用户
     */
    private boolean createUser(HostInfo hostInfo, String username, String groupname) {
        try {
            String command;
            if (StringUtils.isNotBlank(groupname)) {
                command = "useradd -m -g " + groupname + " " + username;
            } else {
                command = "useradd -m " + username;
            }
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);
            return result.isSuccess();
        } catch (Exception e) {
            logger.error("创建用户时发生错误: " + username, e);
            cacheLog.error("创建用户 " + username + " 时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建组
     */
    private boolean createGroup(HostInfo hostInfo, String groupname) {
        try {
            String command = "groupadd " + groupname;
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);
            return result.isSuccess();
        } catch (Exception e) {
            logger.error("创建组时发生错误: " + groupname, e);
            cacheLog.error("创建组 " + groupname + " 时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 为用户找到对应的组
     */
    private String findGroupForUser(HostInfo hostInfo, String username) {
        // 从缓存获取默认用户组映射
        Map<String, String> mappings = getDefaultGroupMappings();

        // 如果配置中有该用户的映射，使用配置的映射
        if (mappings.containsKey(username)) {
            return mappings.get(username);
        }

        // 默认使用与用户名相同的组名
        return username;
    }

    /**
     * 创建失败详情消息
     */
    private StringBuilder createFailDetails(Set<String> existingUsers, Set<String> existingGroups,
            List<String> usersNeedHomeDir) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: SF Pro Text, -apple-system, BlinkMacSystemFont, Helvetica Neue, Helvetica, Arial, sans-serif; ");
        sb.append("background: linear-gradient(to bottom, rgba(249, 249, 249, 0.95), rgba(244, 244, 244, 0.95)); ");
        sb.append("border-radius: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); ");
        sb.append("overflow: hidden; max-width: 100%; padding: 20px;\">");

        // 标题区域
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 16px;\">");
        sb.append(
                "<div style=\"width: 12px; height: 12px; border-radius: 50%; background-color: #ff3b30; margin-right: 10px;\"></div>");
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">用户和组检查失败</div>");
        sb.append("</div>");

        // 统计区域
        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 20px;\">");

        // 用户统计
        int totalUsers = existingUsers.size() + usersToCreate.size();
        int userPercent = totalUsers > 0 ? existingUsers.size() * 100 / totalUsers : 0;

        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">用户</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">").append(existingUsers.size())
                .append("/").append(totalUsers).append("</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: ").append(userPercent)
                .append("%; background: #ff9500; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");

        // 组统计
        int totalGroups = existingGroups.size() + groupsToCreate.size();
        int groupPercent = totalGroups > 0 ? existingGroups.size() * 100 / totalGroups : 0;

        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">组</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">").append(existingGroups.size())
                .append("/").append(totalGroups).append("</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: ").append(groupPercent)
                .append("%; background: #ff9500; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");

        // 已存在用户列表
        if (!existingUsers.isEmpty()) {
            sb.append("<div style=\"margin-bottom: 16px;\">");
            sb.append(
                    "<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">已存在的用户</div>");
            sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
            for (String user : existingUsers) {
                // 检查用户是否需要home目录
                boolean needsHome = usersNeedHomeDir.contains(user);
                String color = needsHome ? "#ff9500" : "#34c759"; // 橙色表示需要创建home目录，绿色表示完全正常
                String bgColor = needsHome ? "rgba(255, 149, 0, 0.15)" : "rgba(52, 199, 89, 0.15)";

                sb.append("<div style=\"font-size: 13px; background: ").append(bgColor).append("; color: ")
                        .append(color).append("; ");
                sb.append("border-radius: 6px; padding: 4px 10px;\">");
                sb.append(user);
                if (needsHome) {
                    sb.append(" (缺少home目录)");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
            sb.append("</div>");
        }

        // 缺失用户列表
        if (!usersToCreate.isEmpty()) {
            sb.append("<div style=\"margin-bottom: 16px;\">");
            sb.append(
                    "<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">缺失的用户</div>");
            sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
            for (String user : usersToCreate) {
                sb.append("<div style=\"font-size: 13px; background: rgba(255, 59, 48, 0.15); color: #ff3b30; ");
                sb.append("border-radius: 6px; padding: 4px 10px;\">").append(user).append("</div>");
            }
            sb.append("</div>");
            sb.append("</div>");
        }

        // 已存在组列表
        if (!existingGroups.isEmpty()) {
            sb.append("<div style=\"margin-bottom: 16px;\">");
            sb.append(
                    "<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">已存在的组</div>");
            sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
            for (String group : existingGroups) {
                sb.append("<div style=\"font-size: 13px; background: rgba(52, 199, 89, 0.15); color: #34c759; ");
                sb.append("border-radius: 6px; padding: 4px 10px;\">").append(group).append("</div>");
            }
            sb.append("</div>");
            sb.append("</div>");
        }

        // 缺失组列表
        if (!groupsToCreate.isEmpty()) {
            sb.append("<div>");
            sb.append(
                    "<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">缺失的组</div>");
            sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
            for (String group : groupsToCreate) {
                sb.append("<div style=\"font-size: 13px; background: rgba(255, 59, 48, 0.15); color: #ff3b30; ");
                sb.append("border-radius: 6px; padding: 4px 10px;\">").append(group).append("</div>");
            }
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");

        // 底部提示
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("需要创建缺失的用户、组和home目录才能继续。点击\"修复\"按钮自动创建必要的资源。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(Set<String> users, Set<String> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: SF Pro Text, -apple-system, BlinkMacSystemFont, Helvetica Neue, Helvetica, Arial, sans-serif; ");
        sb.append("background: linear-gradient(to bottom, rgba(249, 249, 249, 0.95), rgba(244, 244, 244, 0.95)); ");
        sb.append("border-radius: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); ");
        sb.append("overflow: hidden; max-width: 100%; padding: 20px;\">");

        // 标题区域
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 16px;\">");
        sb.append(
                "<div style=\"width: 12px; height: 12px; border-radius: 50%; background-color: #34c759; margin-right: 10px;\"></div>");
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">用户和组检查状态</div>");
        sb.append("</div>");

        // 统计区域
        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 20px;\">");

        // 用户统计
        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">用户</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">").append(users.size())
                .append("</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: 100%; background: #34c759; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");

        // 组统计
        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">组</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">").append(groups.size())
                .append("</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: 100%; background: #5ac8fa; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");

        // 用户列表
        sb.append("<div style=\"margin-bottom: 16px;\">");
        sb.append("<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">已存在的用户</div>");
        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
        for (String user : users) {
            sb.append("<div style=\"font-size: 13px; background: rgba(52, 199, 89, 0.15); color: #34c759; ");
            sb.append("border-radius: 6px; padding: 4px 10px;\">").append(user).append("</div>");
        }
        sb.append("</div>");
        sb.append("</div>");

        // 组列表
        sb.append("<div>");
        sb.append("<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">已存在的组</div>");
        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
        for (String group : groups) {
            sb.append("<div style=\"font-size: 13px; background: rgba(90, 200, 250, 0.15); color: #5ac8fa; ");
            sb.append("border-radius: 6px; padding: 4px 10px;\">").append(group).append("</div>");
        }
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");

        // 底部提示
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("所有必要的用户和组已经准备就绪，服务可以正常启动。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.USER_GROUP_CHECK;
    }
}