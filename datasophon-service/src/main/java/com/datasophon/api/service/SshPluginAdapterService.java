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

package com.datasophon.api.service;

import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.model.CommandResult;
import java.util.List;
import java.util.Map;


/**
 * SSH插件适配器服务接口
 * 
 * 作用：让主程序可以通过插件化方式使用SSH功能，完全隔离直接SSH调用
 * 原则：主程序禁止直接使用任何SSH库，必须通过此适配器调用SSH插件
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
public interface SshPluginAdapterService {
    
    /**
     * 测试SSH连接
     * 替换 MinaUtils.openConnection
     * 
     * @param hostInfo 主机信息
     * @return 连接测试结果
     */
    CommandResult testConnection(HostInfo hostInfo);
    
    /**
     * 执行SSH命令并返回结果
     * 替换 MinaUtils.execCmdWithResult
     * 
     * @param hostInfo 主机信息
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    String executeCommand(HostInfo hostInfo, String command);
    
    /**
     * 执行SSH命令并返回详细结果对象
     * 替换 MinaUtils.execCmdWithResultObject
     * 
     * @param hostInfo 主机信息
     * @param command 要执行的命令
     * @return 命令执行详细结果
     */
    CommandResult executeCommandWithResult(HostInfo hostInfo, String command);
    
    /**
     * 执行SSH命令并返回详细结果对象（带超时）
     * 替换 MinaUtils.execCmdWithResultObject(session, command, timeout)
     * 
     * @param hostInfo 主机信息
     * @param command 要执行的命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 命令执行详细结果
     */
    CommandResult executeCommandWithResult(HostInfo hostInfo, String command, long timeoutSeconds);
    
    /**
     * 上传文件到远程主机
     * 替换 MinaUtils.uploadFile
     * 
     * @param hostInfo 主机信息
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 远程文件路径
     * @return 上传是否成功
     */
    boolean uploadFile(HostInfo hostInfo, String localFilePath, String remoteFilePath);
    
    /**
     * 上传文件流到远程主机
     * 新增功能，支持从InputStream上传
     * 
     * @param hostInfo 主机信息
     * @param inputStream 输入流
     * @param remoteFilePath 远程文件路径
     * @return 上传是否成功
     */
    boolean uploadFile(HostInfo hostInfo, java.io.InputStream inputStream, String remoteFilePath);
    
    /**
     * 从远程主机下载文件
     * 新增功能
     * 
     * @param hostInfo 主机信息
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 本地文件路径
     * @return 下载是否成功
     */
    boolean downloadFile(HostInfo hostInfo, String remoteFilePath, String localFilePath);
    
    /**
     * 创建远程目录
     * 新增功能
     * 
     * @param hostInfo 主机信息
     * @param remotePath 远程目录路径
     * @return 创建是否成功
     */
    boolean createDirectory(HostInfo hostInfo, String remotePath);
    
    /**
     * 删除远程文件
     * 新增功能
     * 
     * @param hostInfo 主机信息
     * @param remoteFilePath 远程文件路径
     * @return 删除是否成功
     */
    boolean deleteFile(HostInfo hostInfo, String remoteFilePath);
    
    /**
     * 检查远程路径是否存在
     * 新增功能
     * 
     * @param hostInfo 主机信息
     * @param remotePath 远程路径
     * @return 路径是否存在
     */
    boolean checkPathExists(HostInfo hostInfo, String remotePath);
    
    /**
     * 创建空文件
     * 新增功能
     * 
     * @param hostInfo 主机信息
     * @param remoteFilePath 远程文件路径
     * @return 创建是否成功
     */
    boolean createFile(HostInfo hostInfo, String remoteFilePath);
    
    /**
     * 检测Linux发行版
     * 替换 MinaUtils.detectLinuxDistro
     * 
     * @param hostInfo 主机信息
     * @return Linux发行版名称
     */
    String detectLinuxDistro(HostInfo hostInfo);
    
    /**
     * 根据发行版适配命令
     * 替换 MinaUtils.adaptCommandToDistro
     * 
     * @param hostInfo 主机信息
     * @param command 原始命令
     * @return 适配后的命令
     */
    String adaptCommandToDistro(HostInfo hostInfo, String command);
    
    /**
     * 安全执行命令（带错误处理）
     * 替换 MinaUtils.safeExecCommand
     * 
     * @param hostInfo 主机信息
     * @param command 要执行的命令
     * @return 命令执行结果，失败时返回空字符串
     */
    String safeExecuteCommand(HostInfo hostInfo, String command);
    
    /**
     * 为Debian系统创建systemd服务
     * 替换 MinaUtils.createSystemdServiceForDebian
     * 
     * @param hostInfo 主机信息
     * @param scriptPath 脚本路径
     * @param installPath 安装路径
     * @return 创建是否成功
     */
    boolean createSystemdServiceForDebian(HostInfo hostInfo, String scriptPath, String installPath);
    
    /**
     * 检查SSH连接是否可用
     * 替换 MinaUtils.isSessionValid
     * 
     * @param hostInfo 主机信息
     * @return 连接是否有效
     */
    boolean isConnectionValid(HostInfo hostInfo);
    
    /**
     * 获取SSH插件连接池统计信息
     * 新增功能，用于监控
     * 
     * @return 连接池统计信息
     */
    Map<String, Object> getConnectionPoolStats();
    
    /**
     * 批量执行命令
     * 新增功能，提升性能
     * 
     * @param hostInfo 主机信息
     * @param commands 命令列表
     * @return 执行结果列表
     */
    List<CommandResult> executeBatchCommands(HostInfo hostInfo, List<String> commands);
}
