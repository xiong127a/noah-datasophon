package com.datasophon.api.utils;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 通用压缩工具类
 * 支持多种压缩格式以及密码保护
 */
@Slf4j
public class CompressUtils {

    /**
     * 压缩类型枚举
     */
    @Getter
    public enum CompressType {
        /** ZIP格式 */
        ZIP("zip"),
        /** 7Z格式 */
        SEVEN_ZIP("7z"),
        /** TAR格式 */
        TAR("tar"),
        /** TAR.GZ格式 */
        TAR_GZ("tar.gz"),
        /** TAR.XZ格式 */
        TAR_XZ("tar.xz"),
        /** GZIP格式 */
        GZIP("gz"),
        /** BZIP2格式 */
        BZIP2("bz2");

        private final String extension;

        CompressType(String extension) {
            this.extension = extension;
        }

        /**
         * 根据字符串获取压缩类型
         */
        public static CompressType fromString(String format) {
            if (StrUtil.isBlank(format)) {
                return ZIP; // 默认ZIP格式
            }

            for (CompressType type : values()) {
                if (type.getExtension().equalsIgnoreCase(format)) {
                    return type;
                }
            }

            // 兼容处理某些特殊情况
            if ("tar.gz".equalsIgnoreCase(format) || "tgz".equalsIgnoreCase(format)) {
                return TAR_GZ;
            } else if ("tar.xz".equalsIgnoreCase(format) || "txz".equalsIgnoreCase(format)) {
                return TAR_XZ;
            } else if ("7z".equalsIgnoreCase(format) || "7zip".equalsIgnoreCase(format)) {
                return SEVEN_ZIP;
            } else if ("tar".equalsIgnoreCase(format)) {
                return TAR;
            }

            return ZIP; // 默认使用ZIP格式
        }
    }

    /**
     * 压缩进度缓存,key为serviceInstanceId,value为进度值(0-100)
     */
    private static final Map<Long, Integer> COMPRESS_PROGRESS_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取压缩进度
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 压缩进度(0-100)
     */
    public static Integer getCompressProgress(Long serviceInstanceId) {
        return COMPRESS_PROGRESS_CACHE.getOrDefault(serviceInstanceId, 0);
    }

    /**
     * 更新压缩进度
     * 
     * @param serviceInstanceId 服务实例ID
     * @param progress          进度值(0-100)
     */
    private static void updateCompressProgress(Long serviceInstanceId, Integer progress) {
        COMPRESS_PROGRESS_CACHE.put(serviceInstanceId, progress);
    }

    /**
     * 清除压缩进度
     * 
     * @param serviceInstanceId 服务实例ID
     */
    private static void clearCompressProgress(Long serviceInstanceId) {
        COMPRESS_PROGRESS_CACHE.remove(serviceInstanceId);
    }

    /**
     * 压缩文件，支持密码保护
     * 
     * @param files             文件名与内容的映射
     * @param type              压缩类型
     * @param password          密码，为空则不加密
     * @param serviceInstanceId 服务实例ID,用于跟踪进度
     * @return 压缩后的字节数组
     */
    public static byte[] compress(Map<String, byte[]> files, CompressType type, String password,
                                  Long serviceInstanceId) {
        if (files == null || files.isEmpty()) {
            log.warn("没有文件需要压缩");
            return new byte[0];
        }

        try {
            // 初始化进度
            updateCompressProgress(serviceInstanceId, 0);

            // 创建临时目录
            Path tempDir = Files.createTempDirectory("compress_temp_");

            // 将文件写入临时目录
            int totalFiles = files.size();
            int processedFiles = 0;
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                byte[] content = entry.getValue();

                if (content != null && content.length > 0) {
                    Path filePath = tempDir.resolve(fileName);
                    // 确保父目录存在
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, content);
                }

                // 更新进度(写入文件阶段占40%)
                processedFiles++;
                int progress = (int) ((processedFiles * 40.0) / totalFiles);
                updateCompressProgress(serviceInstanceId, progress);
            }

            // 根据不同类型执行压缩
            byte[] result = switch (type) {
                case SEVEN_ZIP -> compressToSevenZip(tempDir, password);
                case TAR -> compressToTar(tempDir);
                case TAR_GZ -> compressToTarGz(tempDir);
                case TAR_XZ -> compressToTarXz(tempDir);
                case GZIP -> compressToGzip(tempDir);
                case BZIP2 -> compressToBzip2(tempDir);
                default -> compressToZip(tempDir, password);
            };

            // 更新进度为100%
            updateCompressProgress(serviceInstanceId, 100);

            // 清理临时目录
            cleanTempDir(tempDir);

            return result;
        } catch (Exception e) {
            log.error("压缩文件失败", e);
            // 出错时清除进度
            clearCompressProgress(serviceInstanceId);
            return new byte[0];
        }
    }

    /**
     * 压缩为ZIP格式，支持密码
     */
    private static byte[] compressToZip(Path directory, String password) throws IOException {
        // 判断是否需要使用加密ZIP
        if (StrUtil.isNotBlank(password)) {
            try {
                return compressToEncryptedZip(directory, password);
            } catch (Exception e) {
                log.error("创建加密ZIP失败，使用非加密ZIP替代", e);
            }
        }

        // 使用标准ZIP（无密码）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            // 设置压缩级别
            zipOut.setLevel(Deflater.BEST_COMPRESSION);

            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 获取相对路径
                    Path relativePath = directory.relativize(file);
                    // 创建ZIP条目
                    ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace("\\", "/"));
                    zipOut.putNextEntry(zipEntry);

                    // 写入文件内容
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    /**
     * 创建加密ZIP文件(使用zip4j库)
     */
    private static byte[] compressToEncryptedZip(Path directory, String password) throws Exception {
        // 创建临时zip文件
        Path tempZipFile = Files.createTempFile("encrypted_", ".zip");

        try {
            // 创建zip4j实例
            ZipFile zipFile = new ZipFile(tempZipFile.toFile());

            // 设置密码
            if (StrUtil.isNotBlank(password)) {
                zipFile.setPassword(password.toCharArray());
            }

            // 压缩参数
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setCompressionLevel(CompressionLevel.ULTRA); // 最高压缩级别

            // 设置加密选项
            if (StrUtil.isNotBlank(password)) {
                zipParameters.setEncryptFiles(true);
                zipParameters.setEncryptionMethod(EncryptionMethod.AES);
                zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            }

            // 添加文件到zip
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Path relativePath = directory.relativize(file);
                        String entryName = relativePath.toString().replace("\\", "/");

                        // 为每个文件创建新的参数对象，避免共享问题
                        ZipParameters fileParams = new ZipParameters(zipParameters);
                        fileParams.setFileNameInZip(entryName);

                        // 添加文件
                        zipFile.addFile(file.toFile(), fileParams);
                    } catch (Exception e) {
                        throw new IOException("添加文件到加密ZIP失败", e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 读取zip文件内容
            return Files.readAllBytes(tempZipFile);
        } finally {
            // 删除临时文件
            try {
                Files.deleteIfExists(tempZipFile);
            } catch (Exception e) {
                log.warn("删除临时ZIP文件失败", e);
            }
        }
    }

    /**
     * 压缩为7Z格式
     * 使用Apache Commons Compress库实现标准7z格式
     * 注意：此实现不支持密码保护，即使传入密码参数也会被忽略
     */
    private static byte[] compressToSevenZip(Path directory, String password) throws IOException {
        // 如果有密码参数，记录警告日志但继续使用7z格式
        if (StrUtil.isNotBlank(password)) {
            log.warn("7z格式不支持密码保护，将忽略密码参数");
        }

        try {
            // 创建临时7z文件
            Path tempFile = Files.createTempFile("temp_", ".7z");

            // 使用Commons Compress创建7z文件
            try (SevenZOutputFile sevenZOutput = new SevenZOutputFile(tempFile.toFile())) {
                // 设置LZMA2压缩方法
                sevenZOutput.setContentCompression(SevenZMethod.LZMA2);

                // 添加目录中的所有文件
                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            // 获取相对路径
                            Path relativePath = directory.relativize(file);
                            String entryName = relativePath.toString().replace("\\", "/");

                            // 创建7z条目
                            SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(file.toFile(), entryName);
                            sevenZOutput.putArchiveEntry(entry);

                            // 写入文件内容
                            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                                byte[] buffer = new byte[8192];
                                int len;
                                while ((len = fis.read(buffer)) > 0) {
                                    sevenZOutput.write(buffer, 0, len);
                                }
                            }
                            sevenZOutput.closeArchiveEntry();
                        } catch (Exception e) {
                            throw new IOException("添加文件到7Z归档失败：" + e.getMessage(), e);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // 读取生成的7z文件内容
            byte[] sevenZContent = Files.readAllBytes(tempFile);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return sevenZContent;
        } catch (Exception e) {
            log.error("创建7Z文件失败: {}", e.getMessage(), e);
            log.warn("由于7Z格式压缩失败，系统将使用ZIP格式代替（保留7z后缀）");
            return compressToZip(directory, password);
        }
    }

    /**
     * 压缩为TAR格式（无压缩的归档）
     */
    private static byte[] compressToTar(Path directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(baos)) {
            // 设置长文件名支持模式
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 获取相对路径
                    Path relativePath = directory.relativize(file);

                    // 创建TAR条目
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(),
                            relativePath.toString().replace("\\", "/"));
                    taos.putArchiveEntry(entry);

                    // 写入文件内容
                    Files.copy(file, taos);
                    taos.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    /**
     * 压缩为TAR.GZ格式
     */
    private static byte[] compressToTarGz(Path directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(baos))) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 获取相对路径
                    Path relativePath = directory.relativize(file);

                    // 创建TAR条目
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(),
                            relativePath.toString().replace("\\", "/"));
                    taos.putArchiveEntry(entry);

                    // 写入文件内容
                    Files.copy(file, taos);
                    taos.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    /**
     * 压缩为TAR.XZ格式
     */
    private static byte[] compressToTarXz(Path directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new XZCompressorOutputStream(baos))) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 获取相对路径
                    Path relativePath = directory.relativize(file);

                    // 创建TAR条目
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(),
                            relativePath.toString().replace("\\", "/"));
                    taos.putArchiveEntry(entry);

                    // 写入文件内容
                    Files.copy(file, taos);
                    taos.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    /**
     * 压缩为GZIP格式（单文件压缩）
     */
    private static byte[] compressToGzip(Path directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // GZIP只能压缩单个文件，这里我们创建一个包含所有文件的TAR文件
        Path tarFile = Files.createTempFile("temp_for_gzip_", ".tar");
        try {
            // 先创建TAR文件
            try (TarArchiveOutputStream taos = new TarArchiveOutputStream(Files.newOutputStream(tarFile))) {
                taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relativePath = directory.relativize(file);
                        TarArchiveEntry entry = new TarArchiveEntry(file.toFile(),
                                relativePath.toString().replace("\\", "/"));
                        taos.putArchiveEntry(entry);
                        Files.copy(file, taos);
                        taos.closeArchiveEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // 再用GZIP压缩TAR文件
            try (GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(baos)) {
                Files.copy(tarFile, gzOut);
            }
        } finally {
            // 删除临时TAR文件
            Files.deleteIfExists(tarFile);
        }

        return baos.toByteArray();
    }

    /**
     * 压缩为BZIP2格式（单文件压缩）
     */
    private static byte[] compressToBzip2(Path directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // BZIP2只能压缩单个文件，这里我们创建一个包含所有文件的TAR文件
        Path tarFile = Files.createTempFile("temp_for_bzip2_", ".tar");
        try {
            // 先创建TAR文件
            try (TarArchiveOutputStream taos = new TarArchiveOutputStream(Files.newOutputStream(tarFile))) {
                taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relativePath = directory.relativize(file);
                        TarArchiveEntry entry = new TarArchiveEntry(file.toFile(),
                                relativePath.toString().replace("\\", "/"));
                        taos.putArchiveEntry(entry);
                        Files.copy(file, taos);
                        taos.closeArchiveEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // 再用BZIP2压缩TAR文件
            try (BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(baos)) {
                Files.copy(tarFile, bzOut);
            }
        } finally {
            // 删除临时TAR文件
            Files.deleteIfExists(tarFile);
        }

        return baos.toByteArray();
    }

    /**
     * 清理临时目录
     */
    private static void cleanTempDir(Path tempDir) {
        try {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.warn("清理临时目录失败", e);
        }
    }

    /**
     * 获取带密码的压缩文件内容
     * 
     * @param files             文件映射
     * @param format            压缩格式
     * @param password          密码，可为空
     * @param serviceInstanceId 服务实例ID,用于跟踪进度
     * @return 压缩后的字节数组
     */
    public static byte[] getCompressedFiles(Map<String, byte[]> files, String format, String password,
                                            Long serviceInstanceId) {
        CompressType type = CompressType.fromString(format);
        return compress(files, type, password, serviceInstanceId);
    }
}