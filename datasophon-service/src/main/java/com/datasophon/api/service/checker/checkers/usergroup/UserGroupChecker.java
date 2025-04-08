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
                boolean exists = checkGroupExists(group);
                cacheLog.info("组 " + group + ": " + (exists ? "存在" : "不存在"));
                if (!exists) {
                    groupsToCreate.add(group);
                    checkFailed = true;
                }
            }

            // 检查用户是否存在
            cacheLog.info("\n正在检查用户是否存在...");
            for (String user : users) {
                boolean exists = checkUserExists(user);
                cacheLog.info("用户 " + user + ": " + (exists ? "存在" : "不存在"));
                if (!exists) {
                    usersToCreate.add(user);
                    checkFailed = true;
                }
            }

            // 设置检查状态和消息
            if (checkFailed) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder detailsBuilder = createFailureDetails();
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
                boolean success = createGroup(group);
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
                String group = findGroupForUser(user);
                cacheLog.info("创建用户: " + user + " (组: " + group + ")");
                boolean success = createUser(user, group);
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
    private boolean checkUserExists(String username) throws InterruptedException {
        String command = "id -u " + username + " >/dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult result = execCommand(session, command);
        return result.isSuccess() && "EXISTS".equals(result.getOutput().trim());
    }

    /**
     * 检查组是否存在
     */
    private boolean checkGroupExists(String groupname) throws InterruptedException {
        String command = "getent group " + groupname + " >/dev/null 2>&1 && echo 'EXISTS' || echo 'NOT_EXISTS'";
        CommandResult result = execCommand(session, command);
        return result.isSuccess() && "EXISTS".equals(result.getOutput().trim());
    }

    /**
     * 创建用户
     */
    private boolean createUser(String username, String groupname) {
        try {
            String command;
            if (StringUtils.isNotBlank(groupname)) {
                command = "useradd -m -g " + groupname + " " + username;
            } else {
                command = "useradd -m " + username;
            }
            CommandResult result = execCommand(session, command);
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
    private boolean createGroup(String groupname) {
        try {
            String command = "groupadd " + groupname;
            CommandResult result = execCommand(session, command);
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
    private String findGroupForUser(String username) {
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
    private StringBuilder createFailureDetails() {
        StringBuilder detailsBuilder = new StringBuilder();

        // 使用警告样式
        detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                "发现缺少的用户或组",
                "系统检测到某些服务所需的用户或组不存在"));

        // 添加缺少的用户和组信息
        detailsBuilder.append(HtmlStyleHelper.beginGroup());

        // 添加用户和组状态概览
        detailsBuilder.append("<div style='margin-bottom: 20px;'>");
        detailsBuilder.append("<p style='font-weight: 500; margin-bottom: 10px;'>用户和组状态概览:</p>");

        // 计算用户和组的通过率
        int totalUsers = usersToCreate.size();
        int totalGroups = groupsToCreate.size();
        int totalItems = totalUsers + totalGroups;
        int failedItems = totalItems;
        int passRate = 0;

        if (totalItems > 0) {
            passRate = (totalItems - failedItems) * 100 / totalItems;
        }

        // 添加进度条
        String progressColor = passRate >= 80 ? HtmlStyleHelper.Colors.SUCCESS
                : passRate >= 50 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR;

        detailsBuilder.append(HtmlStyleHelper.generateProgressBar(passRate, progressColor,
                String.format("通过率: %d%%", passRate)));
        detailsBuilder.append("</div>");

        // 添加组信息
        detailsBuilder.append("<div style='margin-bottom: 15px;'>");
        detailsBuilder.append("<p style='font-weight: 500; margin-bottom: 8px;'>组状态详情:</p>");

        // 去除可能的重复项
        List<String> uniqueGroupsToCreate = new ArrayList<>(new HashSet<>(groupsToCreate));

        // 添加缺少的组信息
        if (!uniqueGroupsToCreate.isEmpty()) {
            detailsBuilder.append("<div style='margin-bottom: 10px;'>");
            detailsBuilder.append("<p style='font-weight: 500; color: #FF3B30; margin-bottom: 8px;'>");
            detailsBuilder.append("<a-icon type='close-circle' style='margin-right: 5px;'/>");
            detailsBuilder.append("缺少的组 (").append(uniqueGroupsToCreate.size()).append("):</p>");
            detailsBuilder.append("<ul style='padding-left: 20px; margin-bottom: 10px; list-style-type: none;'>");
            for (String group : uniqueGroupsToCreate) {
                detailsBuilder.append(
                        "<li style='margin-bottom: 5px; padding: 5px 10px; background-color: rgba(255, 59, 48, 0.1); border-radius: 6px;'>");
                detailsBuilder.append("<a-icon type='close-circle' style='margin-right: 5px; color: #FF3B30;'/>");
                detailsBuilder.append(group);
                detailsBuilder.append("</li>");
            }
            detailsBuilder.append("</ul>");
            detailsBuilder.append("</div>");
        }

        // 添加已存在的组信息
        Set<String> existingGroups = new HashSet<>();

        // 不要重复检查已经确定不存在的组
        for (String group : new HashSet<>(groupsToCreate)) {
            try {
                if (checkGroupExists(group)) {
                    existingGroups.add(group);
                }
            } catch (InterruptedException e) {
                logger.error("检查组是否存在时发生错误", e);
            }
        }

        if (!existingGroups.isEmpty()) {
            detailsBuilder.append("<div style='margin-bottom: 10px;'>");
            detailsBuilder.append("<p style='font-weight: 500; color: #34C759; margin-bottom: 8px;'>");
            detailsBuilder.append("<a-icon type='check-circle' style='margin-right: 5px;'/>");
            detailsBuilder.append("已存在的组 (").append(existingGroups.size()).append("):</p>");
            detailsBuilder.append("<ul style='padding-left: 20px; margin-bottom: 10px; list-style-type: none;'>");
            for (String group : existingGroups) {
                detailsBuilder.append(
                        "<li style='margin-bottom: 5px; padding: 5px 10px; background-color: rgba(52, 199, 89, 0.1); border-radius: 6px;'>");
                detailsBuilder.append("<a-icon type='check-circle' style='margin-right: 5px; color: #34C759;'/>");
                detailsBuilder.append(group);
                detailsBuilder.append("</li>");
            }
            detailsBuilder.append("</ul>");
            detailsBuilder.append("</div>");
        }

        detailsBuilder.append("</div>");

        // 添加用户信息
        detailsBuilder.append("<div style='margin-bottom: 15px;'>");
        detailsBuilder.append("<p style='font-weight: 500; margin-bottom: 8px;'>用户状态详情:</p>");

        // 去除可能的重复项
        List<String> uniqueUsersToCreate = new ArrayList<>(new HashSet<>(usersToCreate));

        // 添加缺少的用户信息
        if (!uniqueUsersToCreate.isEmpty()) {
            detailsBuilder.append("<div style='margin-bottom: 10px;'>");
            detailsBuilder.append("<p style='font-weight: 500; color: #FF3B30; margin-bottom: 8px;'>");
            detailsBuilder.append("<a-icon type='close-circle' style='margin-right: 5px;'/>");
            detailsBuilder.append("缺少的用户 (").append(uniqueUsersToCreate.size()).append("):</p>");
            detailsBuilder.append("<ul style='padding-left: 20px; margin-bottom: 10px; list-style-type: none;'>");
            for (String user : uniqueUsersToCreate) {
                detailsBuilder.append(
                        "<li style='margin-bottom: 5px; padding: 5px 10px; background-color: rgba(255, 59, 48, 0.1); border-radius: 6px;'>");
                detailsBuilder.append("<a-icon type='close-circle' style='margin-right: 5px; color: #FF3B30;'/>");
                detailsBuilder.append(user);
                detailsBuilder.append("</li>");
            }
            detailsBuilder.append("</ul>");
            detailsBuilder.append("</div>");
        }

        // 添加已存在的用户信息
        Set<String> existingUsers = new HashSet<>();

        // 不要重复检查已经确定不存在的用户
        for (String user : new HashSet<>(usersToCreate)) {
            try {
                if (checkUserExists(user)) {
                    existingUsers.add(user);
                }
            } catch (InterruptedException e) {
                logger.error("检查用户是否存在时发生错误", e);
            }
        }

        if (!existingUsers.isEmpty()) {
            detailsBuilder.append("<div style='margin-bottom: 10px;'>");
            detailsBuilder.append("<p style='font-weight: 500; color: #34C759; margin-bottom: 8px;'>");
            detailsBuilder.append("<a-icon type='check-circle' style='margin-right: 5px;'/>");
            detailsBuilder.append("已存在的用户 (").append(existingUsers.size()).append("):</p>");
            detailsBuilder.append("<ul style='padding-left: 20px; margin-bottom: 10px; list-style-type: none;'>");
            for (String user : existingUsers) {
                detailsBuilder.append(
                        "<li style='margin-bottom: 5px; padding: 5px 10px; background-color: rgba(52, 199, 89, 0.1); border-radius: 6px;'>");
                detailsBuilder.append("<a-icon type='check-circle' style='margin-right: 5px; color: #34C759;'/>");
                detailsBuilder.append(user);
                detailsBuilder.append("</li>");
            }
            detailsBuilder.append("</ul>");
            detailsBuilder.append("</div>");
        }

        detailsBuilder.append("</div>");

        detailsBuilder.append(HtmlStyleHelper.endGroup());

        // 添加修复建议
        detailsBuilder.append(HtmlStyleHelper.beginGroup());
        detailsBuilder.append("<p style='font-weight: 500; margin-bottom: 8px;'>修复建议:</p>");
        detailsBuilder.append("<ol style='padding-left: 20px; margin-bottom: 15px;'>");
        detailsBuilder.append("<li style='margin-bottom: 5px;'>点击本检查项的修复按钮，系统将自动创建所需的用户和组</li>");
        detailsBuilder.append("<li style='margin-bottom: 5px;'>或手动在系统中创建上述缺少的用户和组</li>");
        detailsBuilder.append("</ol>");
        detailsBuilder.append(HtmlStyleHelper.endGroup());

        return detailsBuilder;
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(Set<String> users, Set<String> groups) {
        StringBuilder detailsBuilder = new StringBuilder();

        // 主容器
        detailsBuilder.append(
                "<div style='background: linear-gradient(to bottom, #ffffff, #f8f8f8); border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 24px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;'>");

        // 顶部状态区域
        detailsBuilder.append("<div style='display: flex; align-items: center; margin-bottom: 24px;'>");
        detailsBuilder.append(
                "<div style='background-color: rgba(52, 199, 89, 0.1); border-radius: 50%; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; margin-right: 16px;'>");
        detailsBuilder.append("<a-icon type='check-circle' style='color: #34C759; font-size: 24px;'/>");
        detailsBuilder.append("</div>");
        detailsBuilder.append("<div>");
        detailsBuilder
                .append("<h3 style='margin: 0; font-size: 20px; font-weight: 600; color: #1d1d1f;'>用户和组检查通过</h3>");
        detailsBuilder.append("<p style='margin: 4px 0 0; font-size: 14px; color: #86868b;'>所有服务所需的用户和组都已存在</p>");
        detailsBuilder.append("</div>");
        detailsBuilder.append("</div>");

        // 进度概览卡片
        detailsBuilder.append(
                "<div style='background-color: #f5f5f7; border-radius: 12px; padding: 20px; margin-bottom: 24px;'>");
        detailsBuilder.append(
                "<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;'>");
        detailsBuilder.append("<span style='font-size: 15px; font-weight: 500; color: #1d1d1f;'>检查完成度</span>");
        detailsBuilder.append("<span style='font-size: 15px; font-weight: 500; color: #34C759;'>100%</span>");
        detailsBuilder.append("</div>");
        detailsBuilder.append(
                "<div style='background-color: rgba(52, 199, 89, 0.2); height: 6px; border-radius: 3px; overflow: hidden;'>");
        detailsBuilder.append(
                "<div style='width: 100%; height: 100%; background-color: #34C759; border-radius: 3px;'></div>");
        detailsBuilder.append("</div>");
        detailsBuilder.append("</div>");

        // 组状态卡片
        detailsBuilder.append(
                "<div style='background-color: #f5f5f7; border-radius: 12px; padding: 20px; margin-bottom: 16px;'>");
        detailsBuilder.append("<div style='display: flex; align-items: center; margin-bottom: 16px;'>");
        detailsBuilder.append("<a-icon type='team' style='color: #34C759; font-size: 18px; margin-right: 8px;'/>");
        detailsBuilder.append("<span style='font-size: 15px; font-weight: 500; color: #1d1d1f;'>组状态详情</span>");
        detailsBuilder.append("</div>");
        detailsBuilder.append("<div style='display: flex; flex-wrap: wrap; gap: 8px;'>");
        for (String group : groups) {
            detailsBuilder.append(
                    "<div style='background-color: #ffffff; border-radius: 8px; padding: 8px 12px; display: flex; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,0.1);'>");
            detailsBuilder.append(
                    "<a-icon type='check-circle' style='color: #34C759; margin-right: 6px; font-size: 14px;'/>");
            detailsBuilder.append("<span style='font-size: 14px; color: #1d1d1f;'>").append(group).append("</span>");
            detailsBuilder.append("</div>");
        }
        detailsBuilder.append("</div>");
        detailsBuilder.append("</div>");

        // 用户状态卡片
        detailsBuilder.append("<div style='background-color: #f5f5f7; border-radius: 12px; padding: 20px;'>");
        detailsBuilder.append("<div style='display: flex; align-items: center; margin-bottom: 16px;'>");
        detailsBuilder.append("<a-icon type='user' style='color: #34C759; font-size: 18px; margin-right: 8px;'/>");
        detailsBuilder.append("<span style='font-size: 15px; font-weight: 500; color: #1d1d1f;'>用户状态详情</span>");
        detailsBuilder.append("</div>");
        detailsBuilder.append("<div style='display: flex; flex-wrap: wrap; gap: 8px;'>");
        for (String user : users) {
            detailsBuilder.append(
                    "<div style='background-color: #ffffff; border-radius: 8px; padding: 8px 12px; display: flex; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,0.1);'>");
            detailsBuilder.append(
                    "<a-icon type='check-circle' style='color: #34C759; margin-right: 6px; font-size: 14px;'/>");
            detailsBuilder.append("<span style='font-size: 14px; color: #1d1d1f;'>").append(user).append("</span>");
            detailsBuilder.append("</div>");
        }
        detailsBuilder.append("</div>");
        detailsBuilder.append("</div>");

        detailsBuilder.append("</div>");

        return detailsBuilder;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.USER_GROUP_CHECK;
    }
}