#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${r"${BASH_SOURCE[0]}"}")" && pwd)"

# 定义 JUICEFS_HOME 变量
JUICEFS_HOME="${r"${SCRIPT_DIR}"}/.."

# 定义其他变量
LOG_DIR="${logDir}"
BIN_DIR="${r"${JUICEFS_HOME}"}/bin"

# 获取系统架构
ARCH=$(uname -m)

# 根据系统架构选择合适的 JuiceFS 二进制文件
if [ "$ARCH" == "x86_64" ]; then
    JUICEFS_BIN="${r"${BIN_DIR}"}/juicefs-x86_64"
elif [ "$ARCH" == "aarch64" ]; then
    JUICEFS_BIN="${r"${BIN_DIR}"}/juicefs-aarch64"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

# 检查 JuiceFS 二进制文件是否存在
if [ ! -f "$JUICEFS_BIN" ]; then
    echo "JuiceFS binary not found at $JUICEFS_BIN"
    exit 1
fi

# 设置日志文件名
LOG_FILE="$LOG_DIR/juicefs-`hostname`.log"

# 创建日志目录（如果不存在）
mkdir -p "$LOG_DIR"

# 启动 JuiceFS 并将输出重定向到日志文件和控制台
"$JUICEFS_BIN" "$@" 2>&1 | tee "$LOG_FILE"

echo "Logs are written to $LOG_FILE"




