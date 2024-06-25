#!/bin/bash

# 获取当前脚本所在目录
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 切换到脚本所在目录
cd "$script_dir"

# 调用 neo4j status 命令
sh neo4j status

# 检查命令执行结果
if [ $? -eq 0 ]; then
  echo "neo4j is running."
  exit 0
else
  echo "neo4j is not running or an error occurred."
  exit 1
fi