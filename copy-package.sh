#!/bin/bash

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 定义源和目标路径（使用相对路径）
SOURCE_PATH="${SCRIPT_DIR}/datasophon-worker/target/datasophon-worker.tar.gz"
TARGET_DIR="/opt/datasophon/DDP/packages"
TARGET_FILE="$TARGET_DIR/datasophon-worker.tar.gz"
MD5_FILE="$TARGET_FILE.md5"

# 确保目标目录存在
if [ ! -d "$TARGET_DIR" ]; then
    echo "创建目标目录: $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
fi

# 确保源文件存在
if [ ! -f "$SOURCE_PATH" ]; then
    echo "错误: 源文件不存在: $SOURCE_PATH"
    echo "请先运行Maven构建"
    exit 1
fi

# 复制文件
echo "复制文件: $SOURCE_PATH -> $TARGET_FILE"
cp "$SOURCE_PATH" "$TARGET_FILE"

# 生成MD5并写入文件
echo "生成MD5值..."
md5sum "$TARGET_FILE" | awk '{print $1}' > "$MD5_FILE"

echo "完成!"
echo "目标文件: $TARGET_FILE"
echo "MD5文件: $MD5_FILE"
echo "MD5值: $(cat $MD5_FILE)" 