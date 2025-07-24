package com.datasophon.kubernetes.util;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.PropertyUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;

import static com.datasophon.kubernetes.util.KubernetesMinaUtils.uploadFile;

@Slf4j
public class KubernetesKerberosUtils {

    public static void downloadKeytabFromMaster(String hostname, String principal, String keytabName) {
        String masterHost = PropertyUtils.getString(Constants.MASTER_HOST).split(",")[0];
        String masterPort = PropertyUtils.getString(Constants.MASTER_WEB_PORT);
        int clusterId = PropertyUtils.getInt("clusterId");

        // get kerberos keytab
        String downloadUrl = "http://" + masterHost + ":" + masterPort
                + "/ddh/cluster/kerberos/downloadKeytab?clusterId="
                + clusterId + "&principal=" + principal + "&keytabName=" + keytabName + "&hostname=" + hostname;

        String dest = "/etc/security/keytab/";
        try {
            try (InputStream fileStream = downloadFileAsStream(downloadUrl)) {
                System.out.println("File downloaded successfully.");

                // Step 2: Upload the file from InputStream
                boolean uploadSuccess = uploadFile(hostname, dest, fileStream, keytabName);
                if (uploadSuccess) {
                    System.out.println("File uploaded successfully.");
                } else {
                    System.out.println("File upload failed.");
                }
            }
        } catch (IOException e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
        }
    }

    public static void createKeytabDir(String hostname) throws IOException {
        if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/")) {
            FileUtil.mkdir("/etc/security/keytab/");
            KubernetesMinaUtils.createDir(hostname, "/etc/security/keytab/");
        }
        KubernetesMinaUtils.execCmdWithResult(hostname, "chown -R root:hadoop /etc/security/keytab/");
        KubernetesMinaUtils.execCmdWithResult(hostname, "chmod -R 770 /etc/security/keytab/");
    }

    private static InputStream downloadFileAsStream(String downloadUrl) throws IOException {
        URL url = URI.create(downloadUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Return the InputStream directly
        return connection.getInputStream();
    }
}
