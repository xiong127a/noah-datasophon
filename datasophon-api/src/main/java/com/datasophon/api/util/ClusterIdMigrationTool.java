package com.datasophon.api.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集群ID迁移工具
 * 自动将Controller中的 @RequestParam("clusterId") 和 @PathVariable("clusterId") 
 * 替换为 @ClusterId 注解，并添加必要的import
 * 
 * @author DataSophon Team
 */
public class ClusterIdMigrationTool {
    
    private static final String CLUSTER_ID_ANNOTATION_IMPORT = "import com.datasophon.api.annotation.ClusterId;";
    
    // 匹配 @RequestParam("clusterId") 的正则表达式
    private static final Pattern REQUEST_PARAM_PATTERN = 
        Pattern.compile("@RequestParam\\(\\s*[\"']clusterId[\"']\\s*\\)\\s+(\\w+)\\s+(\\w+)");
    
    // 匹配 @PathVariable("clusterId") 的正则表达式  
    private static final Pattern PATH_VARIABLE_PATTERN = 
        Pattern.compile("@PathVariable\\(\\s*[\"']clusterId[\"']\\s*\\)\\s+(\\w+)\\s+(\\w+)");
    
    // 匹配其他带注解的 clusterId 参数
    private static final Pattern OTHER_PARAM_PATTERN = 
        Pattern.compile("@\\w+\\([^)]*\\)\\s+@RequestParam\\(\\s*[\"']clusterId[\"']\\s*\\)\\s+(\\w+)\\s+(\\w+)");
    
    // 匹配 @RequestParam(name = "clusterId") 的正则表达式
    private static final Pattern REQUEST_PARAM_NAME_PATTERN = 
        Pattern.compile("@RequestParam\\(\\s*name\\s*=\\s*[\"']clusterId[\"'].*?\\)\\s+(\\w+)\\s+(\\w+)");
    
    public static void main(String[] args) {
        System.out.println("=== 集群ID迁移工具启动 ===");
        
        String controllersPath = "datasophon-api/src/main/java/com/datasophon/api/controller";
        
        try {
            List<Path> controllerFiles = findControllerFiles(controllersPath);
            System.out.println("找到 " + controllerFiles.size() + " 个Controller文件");
            
            int totalMigrated = 0;
            for (Path file : controllerFiles) {
                int migrated = migrateControllerFile(file);
                totalMigrated += migrated;
                if (migrated > 0) {
                    System.out.println("✅ " + file.getFileName() + " - 迁移了 " + migrated + " 个方法");
                }
            }
            
            System.out.println("=== 迁移完成 ===");
            System.out.println("总共迁移了 " + totalMigrated + " 个方法参数");
            
        } catch (Exception e) {
            System.err.println("迁移过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 查找所有Controller文件
     */
    private static List<Path> findControllerFiles(String controllersPath) throws IOException {
        List<Path> controllerFiles = new ArrayList<>();
        Path controllersDir = Paths.get(controllersPath);
        
        if (!Files.exists(controllersDir)) {
            throw new IOException("Controllers目录不存在: " + controllersPath);
        }
        
        Files.walk(controllersDir)
            .filter(path -> path.toString().endsWith("Controller.java"))
            .forEach(controllerFiles::add);
        
        return controllerFiles;
    }
    
    /**
     * 迁移单个Controller文件
     */
    private static int migrateControllerFile(Path file) throws IOException {
        String content = Files.readString(file);
        String originalContent = content;
        int migratedCount = 0;
        
        // 检查是否需要添加ClusterId import
        boolean needsImport = false;
        
        // 替换 @RequestParam("clusterId") 
        Matcher requestParamMatcher = REQUEST_PARAM_PATTERN.matcher(content);
        while (requestParamMatcher.find()) {
            String type = requestParamMatcher.group(1);
            String paramName = requestParamMatcher.group(2);
            String replacement = "@ClusterId " + type + " " + paramName;
            content = content.replace(requestParamMatcher.group(0), replacement);
            needsImport = true;
            migratedCount++;
        }
        
        // 替换 @RequestParam(name = "clusterId", ...)
        Matcher requestParamNameMatcher = REQUEST_PARAM_NAME_PATTERN.matcher(content);
        while (requestParamNameMatcher.find()) {
            String type = requestParamNameMatcher.group(1);
            String paramName = requestParamNameMatcher.group(2);
            String replacement = "@ClusterId " + type + " " + paramName;
            content = content.replace(requestParamNameMatcher.group(0), replacement);
            needsImport = true;
            migratedCount++;
        }
        
        // 替换 @PathVariable("clusterId")
        Matcher pathVariableMatcher = PATH_VARIABLE_PATTERN.matcher(content);
        while (pathVariableMatcher.find()) {
            String type = pathVariableMatcher.group(1);
            String paramName = pathVariableMatcher.group(2);
            String replacement = "@ClusterId " + type + " " + paramName;
            content = content.replace(pathVariableMatcher.group(0), replacement);
            needsImport = true;
            migratedCount++;
        }
        
        // 添加ClusterId import（如果需要且不存在）
        if (needsImport && !content.contains("import com.datasophon.api.annotation.ClusterId;")) {
            // 找到package声明后的第一个import位置
            Pattern packagePattern = Pattern.compile("(package [^;]+;)");
            Matcher packageMatcher = packagePattern.matcher(content);
            if (packageMatcher.find()) {
                String packageDeclaration = packageMatcher.group(1);
                content = content.replace(packageDeclaration, 
                    packageDeclaration + "\n\n" + CLUSTER_ID_ANNOTATION_IMPORT);
            }
        }
        
        // 只有在内容发生变化时才写回文件
        if (!content.equals(originalContent)) {
            Files.writeString(file, content);
        }
        
        return migratedCount;
    }
    
    /**
     * 生成迁移报告
     */
    public static void generateMigrationReport(String controllersPath) {
        System.out.println("\n=== 迁移前分析报告 ===");
        
        try {
            List<Path> controllerFiles = findControllerFiles(controllersPath);
            int totalMethods = 0;
            
            for (Path file : controllerFiles) {
                String content = Files.readString(file);
                
                int requestParamCount = countMatches(content, REQUEST_PARAM_PATTERN);
                int pathVariableCount = countMatches(content, PATH_VARIABLE_PATTERN);
                int requestParamNameCount = countMatches(content, REQUEST_PARAM_NAME_PATTERN);
                
                int fileTotal = requestParamCount + pathVariableCount + requestParamNameCount;
                if (fileTotal > 0) {
                    System.out.println(file.getFileName() + ": " + fileTotal + " 个需要迁移的方法");
                    totalMethods += fileTotal;
                }
            }
            
            System.out.println("总计: " + totalMethods + " 个方法需要迁移");
            
        } catch (IOException e) {
            System.err.println("生成报告时出错: " + e.getMessage());
        }
    }
    
    private static int countMatches(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}