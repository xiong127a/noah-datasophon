package com.datasophon.common.utils;

import cn.hutool.system.SystemUtil;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 密码加密解密工具类
 */
public class PasswordEncryptionUtils {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncryptionUtils.class);

    // 默认加密密钥，建议在生产环境通过环境变量或启动参数配置
    private static final String DEFAULT_ENCRYPT_KEY = SystemUtil.get("ENCRYPT_KEY");

    // 加密前缀和后缀，用于识别是否为加密字符串
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    /**
     * 创建加密器
     * 
     * @param password 加密密钥
     * @return 加密器
     */
    private static PooledPBEStringEncryptor createEncryptor(String password) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.NoIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 加密字符串
     * 
     * @param plainText  明文
     * @param encryptKey 加密密钥，如为null则使用默认密钥
     * @return 加密后的字符串
     */
    public static String encrypt(String plainText, String encryptKey) {
        if (plainText == null) {
            return null;
        }

        try {
            String key = encryptKey != null ? encryptKey : DEFAULT_ENCRYPT_KEY;
            PooledPBEStringEncryptor encryptor = createEncryptor(key);
            return PREFIX + encryptor.encrypt(plainText) + SUFFIX;
        } catch (Exception e) {
            logger.error("加密失败", e);
            return plainText;
        }
    }

    /**
     * 使用默认密钥加密
     * 
     * @param plainText 明文
     * @return 加密后的字符串
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, DEFAULT_ENCRYPT_KEY);
    }

    /**
     * 解密字符串
     * 
     * @param encryptedText 加密文本
     * @param encryptKey    加密密钥，如为null则使用默认密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String encryptKey) {
        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            String key = encryptKey != null ? encryptKey : DEFAULT_ENCRYPT_KEY;
            PooledPBEStringEncryptor encryptor = createEncryptor(key);
            // 提取加密部分
            String encryptedValue = extractEncryptedValue(encryptedText);
            return encryptor.decrypt(encryptedValue);
        } catch (Exception e) {
            logger.error("解密失败", e);
            return encryptedText;
        }
    }

    /**
     * 使用默认密钥解密
     * 
     * @param encryptedText 加密文本
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, DEFAULT_ENCRYPT_KEY);
    }

    /**
     * 判断字符串是否为加密字符串
     * 
     * @param text 待检测的字符串
     * @return 是否为加密字符串
     */
    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith(PREFIX) && text.endsWith(SUFFIX);
    }

    /**
     * 从加密字符串中提取加密部分
     * 
     * @param encryptedText 加密字符串
     * @return 加密部分
     */
    private static String extractEncryptedValue(String encryptedText) {
        return encryptedText.substring(PREFIX.length(), encryptedText.length() - SUFFIX.length());
    }

    /**
     * 命令行加密工具
     * 参数：
     * -e [text] 加密文本
     * -d [text] 解密文本
     * -k [key] 指定密钥(可选)
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java PasswordEncryptionUtils -e|-d <text> [-k <key>]");
            return;
        }

        String mode = args[0];
        String text = args[1];
        String key = null;

        if (args.length > 3 && "-k".equals(args[2])) {
            key = args[3];
        }

        switch (mode) {
            case "-e":
                System.out.println("Encrypted: " + encrypt(text, key));
                break;
            case "-d":
                System.out.println("Decrypted: " + decrypt(text, key));
                break;
            default:
                System.out.println("Invalid mode. Use -e for encrypt or -d for decrypt.");
        }
    }
}