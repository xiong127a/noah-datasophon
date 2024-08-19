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

package com.datasophon.k8s.actor;

import akka.actor.UntypedActor;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.command.K8sGetLogCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.k8s.util.K8sMinaUtils;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.HashMap;

public class K8sLogActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(K8sLogActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof K8sGetLogCommand) {
            logger.info("get query log command");
            K8sGetLogCommand command = (K8sGetLogCommand) msg;
            HashMap<String, String> paramMap = new HashMap<>();
            paramMap.put("${user}", "root");
            paramMap.put("${host}", command.getHostname());
            String logFileName =
                    PlaceholderUtils.replacePlaceholders(command.getLogFile(), paramMap, Constants.REGEX_VARIABLE);

            ExecResult execResult = new ExecResult();
            String logStr = "can not find log file";
            try (ClientSession clientSession = K8sMinaUtils.openConnection(command.getHostname(), 22, Constants.ROOT);
                 SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(clientSession)) {
                if (logFileName.startsWith(StrUtil.SLASH) && K8sMinaUtils.checkPathExists(sftp, logFileName)) {
                    logStr = K8sMinaUtils.readLastRows(sftp, logFileName, Charset.defaultCharset(), PropertyUtils.getInt("rows"));
                } else if (K8sMinaUtils.checkPathExists(sftp,
                        Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH + logFileName)) {
                    logStr = K8sMinaUtils
                            .readLastRows(sftp,
                                    Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH + logFileName,
                                    Charset.defaultCharset(), PropertyUtils.getInt("rows"));
                }
            }

            execResult.setExecResult(true);
            execResult.setExecOut(logStr);
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(msg);
        }
    }
}
