package com.datasophon.k8s.util;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.PropertyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.datasophon.k8s.util.K8sMinaUtils.uploadFile;

public class K8sKerberosUtils {

    public static void downloadKeytabFromMaster(String hostname, String principal, String keytabName) {
        String masterHost = PropertyUtils.getString(Constants.MASTER_HOST).split(",")[0];
        String masterPort = PropertyUtils.getString(Constants.MASTER_WEB_PORT);
        Integer clusterId = PropertyUtils.getInt("clusterId");

        // get kerberos keytab
        String downloadUrl =
                "http://" +  masterHost+ ":" + masterPort + "/ddh/cluster/kerberos/downloadKeytab?clusterId="
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
            e.printStackTrace();
        }
    }

    public static void createKeytabDir(String hostname) throws IOException {
        if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/")) {
            FileUtil.mkdir("/etc/security/keytab/");
            K8sMinaUtils.createDir(hostname, "/etc/security/keytab/");
        }
        K8sMinaUtils.execCmdWithResult(hostname, "chown -R root:hadoop /etc/security/keytab/");
        K8sMinaUtils.execCmdWithResult(hostname, "chmod -R 770 /etc/security/keytab/");
    }
    private static InputStream downloadFileAsStream(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Return the InputStream directly
        return connection.getInputStream();
    }
}
