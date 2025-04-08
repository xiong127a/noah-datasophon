package com.datasophon.api.service.checker.checkers.usergroup;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.ArrayList;
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
    private boolean checkFailed = false;

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
            checkFailed = false;

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

            // 检查用户是否存在
            cacheLog.info("\n正在检查用户是否存在...");
            for (String user : users) {
                boolean exists = checkUserExists(hostInfo, user);
                cacheLog.info("用户 " + user + ": " + (exists ? "存在" : "不存在"));
                if (!exists) {
                    usersToCreate.add(user);
                    checkFailed = true;
                }
            }

            // 设置检查状态和消息
            if (checkFailed) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder detailsBuilder = createFailDetails(users, groups);
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

        cacheLog.info("==== 用户和组修复结束 ====");
        return allFixed;
    }

    /**
     * 扫描服务配置文件，提取所有runAs字段中的用户和组
     */
    private void scanServiceConfigurations(Set<String> users, Set<String> groups) {
        try {
            // 获取配置的版本，如DDP-1.2.1
            String versionDir = checkerProperties.getMeta().getVersions();

            // 构建完整的搜索路径
            String searchDir = metaBasePath + File.separator + versionDir;

            cacheLog.info("扫描元数据目录: " + searchDir);

            File dir = new File(searchDir);
            if (!dir.exists() || !dir.isDirectory()) {
                cacheLog.warn("元数据目录不存在: " + searchDir);
                cacheLog.info("将使用默认用户组映射配置进行检查");
                addDefaultUserGroupMappings(users, groups);
                return;
            }

            // 递归查找所有service_ddl.json文件
            List<File> serviceDdlFiles = new ArrayList<>();
            findServiceDdlFiles(dir, serviceDdlFiles);

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
     * 检查用户是否存在
     */
    private boolean checkUserExists(HostInfo hostInfo, String username) throws InterruptedException {
        String command = "id -u " + username + " >/dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);
        return result.isSuccess() && "EXISTS".equals(result.getOutput().trim());
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
    private StringBuilder createFailDetails(Set<String> existingUsers, Set<String> existingGroups) {
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
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">" + existingUsers.size() + "/"
                + totalUsers + "</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: " + userPercent
                + "%; background: #ff9500; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");

        // 组统计
        int totalGroups = existingGroups.size() + groupsToCreate.size();
        int groupPercent = totalGroups > 0 ? existingGroups.size() * 100 / totalGroups : 0;

        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">组</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">" + existingGroups.size() + "/"
                + totalGroups + "</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: " + groupPercent
                + "%; background: #ff9500; border-radius: 3px;\"></div>");
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
                sb.append("<div style=\"font-size: 13px; background: rgba(52, 199, 89, 0.15); color: #34c759; ");
                sb.append("border-radius: 6px; padding: 4px 10px;\">" + user + "</div>");
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
                sb.append("border-radius: 6px; padding: 4px 10px;\">" + user + "</div>");
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
                sb.append("border-radius: 6px; padding: 4px 10px;\">" + group + "</div>");
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
                sb.append("border-radius: 6px; padding: 4px 10px;\">" + group + "</div>");
            }
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");

        // 底部提示
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("需要创建缺失的用户和组才能继续。点击\"修复\"按钮自动创建必要的用户和组。");
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
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">" + users.size() + "</div>");
        sb.append("<div style=\"height: 6px; background: #e5e5e5; border-radius: 3px; margin-top: 10px;\">");
        sb.append("<div style=\"height: 6px; width: 100%; background: #34c759; border-radius: 3px;\"></div>");
        sb.append("</div>");
        sb.append("</div>");

        // 组统计
        sb.append("<div style=\"flex: 1; min-width: 200px;\">");
        sb.append("<div style=\"font-size: 14px; color: #86868b; margin-bottom: 6px;\">组</div>");
        sb.append("<div style=\"font-size: 28px; font-weight: 600; color: #1d1d1f;\">" + groups.size() + "</div>");
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
            sb.append("border-radius: 6px; padding: 4px 10px;\">" + user + "</div>");
        }
        sb.append("</div>");
        sb.append("</div>");

        // 组列表
        sb.append("<div>");
        sb.append("<div style=\"font-size: 15px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">已存在的组</div>");
        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 8px;\">");
        for (String group : groups) {
            sb.append("<div style=\"font-size: 13px; background: rgba(90, 200, 250, 0.15); color: #5ac8fa; ");
            sb.append("border-radius: 6px; padding: 4px 10px;\">" + group + "</div>");
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