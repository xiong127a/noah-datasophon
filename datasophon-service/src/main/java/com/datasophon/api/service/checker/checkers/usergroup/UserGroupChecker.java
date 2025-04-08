package com.datasophon.api.service.checker.checkers.usergroup;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.RunAs;
import org.apache.commons.collections.CollectionUtils;
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
    private List<String> usersToCreate = new ArrayList<>();
    private List<String> groupsToCreate = new ArrayList<>();
    private boolean checkFailed = false;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 用户和组检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

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
                return;
            }

            // 递归查找所有service_ddl.json文件
            List<File> serviceDdlFiles = new ArrayList<>();
            findServiceDdlFiles(dir, serviceDdlFiles);

            cacheLog.info("找到 " + serviceDdlFiles.size() + " 个服务配置文件");

            // 解析每个文件
            for (File file : serviceDdlFiles) {
                parseServiceDdlFile(file, users, groups);
            }
        } catch (Exception e) {
            logger.error("扫描服务配置文件时发生错误", e);
            cacheLog.error("扫描服务配置文件时发生错误: " + e.getMessage());
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
        // 从配置获取默认用户组映射
        Map<String, String> defaultGroupMappings = checkerProperties.getUserGroup().getDefaultGroupMappings();

        // 如果配置中有该用户的映射，使用配置的映射
        if (defaultGroupMappings.containsKey(username)) {
            return defaultGroupMappings.get(username);
        }

        // 默认使用与用户名相同的组名
        return username;
    }

    /**
     * 创建失败详情消息
     */
    private StringBuilder createFailureDetails() {
        StringBuilder detailsBuilder = new StringBuilder();

        detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                "发现缺少的用户或组",
                "系统检测到某些服务所需的用户或组不存在"));

        // 添加缺少的用户和组信息
        detailsBuilder.append(HtmlStyleHelper.beginGroup());

        if (!groupsToCreate.isEmpty()) {
            detailsBuilder.append("<p><strong>缺少的组:</strong></p>");
            detailsBuilder.append("<ul style='padding-left:20px;margin-bottom:15px'>");
            for (String group : groupsToCreate) {
                detailsBuilder.append("<li>").append(group).append("</li>");
            }
            detailsBuilder.append("</ul>");
        }

        if (!usersToCreate.isEmpty()) {
            detailsBuilder.append("<p><strong>缺少的用户:</strong></p>");
            detailsBuilder.append("<ul style='padding-left:20px;margin-bottom:15px'>");
            for (String user : usersToCreate) {
                detailsBuilder.append("<li>").append(user).append("</li>");
            }
            detailsBuilder.append("</ul>");
        }

        detailsBuilder.append(HtmlStyleHelper.endGroup());

        // 添加修复建议
        detailsBuilder.append(HtmlStyleHelper.beginGroup());
        detailsBuilder.append("<p><strong>修复建议:</strong></p>");
        detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
        detailsBuilder.append("<li>点击本检查项的修复按钮，系统将自动创建所需的用户和组</li>");
        detailsBuilder.append("<li>或手动在系统中创建上述缺少的用户和组</li>");
        detailsBuilder.append("</ol>");
        detailsBuilder.append(HtmlStyleHelper.endGroup());

        return detailsBuilder;
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(Set<String> users, Set<String> groups) {
        StringBuilder detailsBuilder = new StringBuilder();

        detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                "用户和组检查通过",
                "所有服务所需的用户和组都已存在"));

        // 添加用户和组信息
        detailsBuilder.append(HtmlStyleHelper.beginGroup());

        detailsBuilder.append("<p><strong>检查的组:</strong></p>");
        detailsBuilder.append("<ul style='padding-left:20px;margin-bottom:15px'>");
        for (String group : groups) {
            detailsBuilder.append("<li>").append(group).append(" ✓</li>");
        }
        detailsBuilder.append("</ul>");

        detailsBuilder.append("<p><strong>检查的用户:</strong></p>");
        detailsBuilder.append("<ul style='padding-left:20px;margin-bottom:15px'>");
        for (String user : users) {
            detailsBuilder.append("<li>").append(user).append(" ✓</li>");
        }
        detailsBuilder.append("</ul>");

        detailsBuilder.append(HtmlStyleHelper.endGroup());

        return detailsBuilder;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.USER_GROUP_CHECK;
    }
}