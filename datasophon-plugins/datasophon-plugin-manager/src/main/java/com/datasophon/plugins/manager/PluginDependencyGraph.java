package com.datasophon.plugins.manager;

import lombok.Data;

import java.util.*;

/**
 * 插件依赖图
 * 用于管理和分析插件之间的依赖关系
 * 
 * @author DataSophon Team
 */
@Data
public class PluginDependencyGraph {
    
    /**
     * 插件依赖映射：pluginId -> List<dependencyPluginId>
     */
    private Map<String, List<String>> dependencies = new HashMap<>();
    
    /**
     * 反向依赖映射：pluginId -> List<dependentPluginId>
     */
    private Map<String, List<String>> dependents = new HashMap<>();
    
    /**
     * 添加插件及其依赖
     */
    public void addPlugin(String pluginId, List<String> pluginDependencies) {
        dependencies.put(pluginId, new ArrayList<>(pluginDependencies));
        
        // 更新反向依赖映射
        for (String dependency : pluginDependencies) {
            dependents.computeIfAbsent(dependency, k -> new ArrayList<>()).add(pluginId);
        }
        
        // 确保插件在依赖映射中存在
        dependents.computeIfAbsent(pluginId, k -> new ArrayList<>());
    }
    
    /**
     * 获取插件的直接依赖
     */
    public List<String> getDirectDependencies(String pluginId) {
        return dependencies.getOrDefault(pluginId, new ArrayList<>());
    }
    
    /**
     * 获取插件的所有依赖（递归）
     */
    public Set<String> getAllDependencies(String pluginId) {
        Set<String> allDeps = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectDependencies(pluginId, allDeps, visited);
        return allDeps;
    }
    
    /**
     * 递归收集所有依赖
     */
    private void collectDependencies(String pluginId, Set<String> allDeps, Set<String> visited) {
        if (visited.contains(pluginId)) {
            return; // 避免循环依赖
        }
        
        visited.add(pluginId);
        List<String> directDeps = getDirectDependencies(pluginId);
        
        for (String dependency : directDeps) {
            if (allDeps.add(dependency)) {
                collectDependencies(dependency, allDeps, visited);
            }
        }
    }
    
    /**
     * 获取依赖于指定插件的其他插件
     */
    public List<String> getDependentPlugins(String pluginId) {
        return dependents.getOrDefault(pluginId, new ArrayList<>());
    }
    
    /**
     * 获取所有依赖于指定插件的其他插件（递归）
     */
    public Set<String> getAllDependentPlugins(String pluginId) {
        Set<String> allDependents = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectDependents(pluginId, allDependents, visited);
        return allDependents;
    }
    
    /**
     * 递归收集所有依赖者
     */
    private void collectDependents(String pluginId, Set<String> allDependents, Set<String> visited) {
        if (visited.contains(pluginId)) {
            return;
        }
        
        visited.add(pluginId);
        List<String> directDependents = getDependentPlugins(pluginId);
        
        for (String dependent : directDependents) {
            if (allDependents.add(dependent)) {
                collectDependents(dependent, allDependents, visited);
            }
        }
    }
    
    /**
     * 检查是否存在循环依赖
     */
    public List<List<String>> detectCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String pluginId : dependencies.keySet()) {
            if (!visited.contains(pluginId)) {
                List<String> currentPath = new ArrayList<>();
                detectCycles(pluginId, visited, recursionStack, currentPath, cycles);
            }
        }
        
        return cycles;
    }
    
    /**
     * DFS检测循环依赖
     */
    private void detectCycles(String pluginId, Set<String> visited, Set<String> recursionStack, 
                             List<String> currentPath, List<List<String>> cycles) {
        visited.add(pluginId);
        recursionStack.add(pluginId);
        currentPath.add(pluginId);
        
        List<String> deps = getDirectDependencies(pluginId);
        for (String dependency : deps) {
            if (!visited.contains(dependency)) {
                detectCycles(dependency, visited, recursionStack, new ArrayList<>(currentPath), cycles);
            } else if (recursionStack.contains(dependency)) {
                // 找到循环依赖
                List<String> cycle = new ArrayList<>(currentPath);
                int startIndex = cycle.indexOf(dependency);
                cycles.add(cycle.subList(startIndex, cycle.size()));
            }
        }
        
        recursionStack.remove(pluginId);
    }
    
    /**
     * 获取拓扑排序结果（加载顺序）
     */
    public List<String> getTopologicalOrder() throws CyclicDependencyException {
        // 检查循环依赖
        List<List<String>> cycles = detectCircularDependencies();
        if (!cycles.isEmpty()) {
            throw new CyclicDependencyException("检测到循环依赖: " + cycles);
        }
        
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (String pluginId : dependencies.keySet()) {
            if (!visited.contains(pluginId)) {
                topologicalSort(pluginId, visited, visiting, result);
            }
        }
        
        Collections.reverse(result); // 反转得到正确的加载顺序
        return result;
    }
    
    /**
     * DFS拓扑排序
     */
    private void topologicalSort(String pluginId, Set<String> visited, Set<String> visiting, List<String> result) {
        if (visiting.contains(pluginId)) {
            return; // 正在访问中，跳过
        }
        
        if (visited.contains(pluginId)) {
            return; // 已访问过
        }
        
        visiting.add(pluginId);
        
        List<String> deps = getDirectDependencies(pluginId);
        for (String dependency : deps) {
            topologicalSort(dependency, visited, visiting, result);
        }
        
        visiting.remove(pluginId);
        visited.add(pluginId);
        result.add(pluginId);
    }
    
    /**
     * 获取卸载顺序（与加载顺序相反）
     */
    public List<String> getUnloadOrder() throws CyclicDependencyException {
        List<String> loadOrder = getTopologicalOrder();
        Collections.reverse(loadOrder);
        return loadOrder;
    }
    
    /**
     * 检查插件是否可以安全卸载
     */
    public boolean canSafelyUnload(String pluginId) {
        List<String> dependents = getDependentPlugins(pluginId);
        return dependents.isEmpty();
    }
    
    /**
     * 获取卸载插件时需要同时卸载的其他插件
     */
    public Set<String> getPluginsToUnloadWith(String pluginId) {
        return getAllDependentPlugins(pluginId);
    }
    
    /**
     * 循环依赖异常
     */
    public static class CyclicDependencyException extends Exception {
        public CyclicDependencyException(String message) {
            super(message);
        }
    }
}