#!/bin/bash
# 检查并创建元数据目录脚本

echo "==============================================="
echo "      Noah大数据平台元数据目录检查工具"
echo "==============================================="
echo

# 获取当前目录
CURRENT_DIR=$(pwd)
echo "当前目录: $CURRENT_DIR"

# 尝试确定项目根目录
if [ -d "$CURRENT_DIR/datasophon-api" ]; then
  echo "当前目录是项目根目录"
  PROJECT_ROOT=$CURRENT_DIR
else
  # 检查是否在子模块中
  cd ..
  if [ -d "$(pwd)/datasophon-api" ]; then
    echo "当前在子模块中，已找到项目根目录"
    PROJECT_ROOT=$(pwd)
  else
    echo "无法确定项目根目录，请在项目根目录或其子模块中运行此脚本"
    exit 1
  fi
fi

echo "项目根目录: $PROJECT_ROOT"
echo

# 设置可能的元数据目录路径
API_META_DIR="$PROJECT_ROOT/datasophon-api/src/main/resources/meta"
SERVICE_META_DIR="$PROJECT_ROOT/datasophon-service/src/main/resources/meta"
ROOT_META_DIR="$PROJECT_ROOT/meta"
CONF_META_DIR="$PROJECT_ROOT/conf/meta"

# 检查所有可能的路径
echo "检查可能的元数据目录位置..."

if [ -d "$API_META_DIR" ]; then
  echo "[存在] $API_META_DIR"
  META_DIR=$API_META_DIR
elif [ -d "$SERVICE_META_DIR" ]; then
  echo "[存在] $SERVICE_META_DIR"
  META_DIR=$SERVICE_META_DIR
elif [ -d "$ROOT_META_DIR" ]; then
  echo "[存在] $ROOT_META_DIR"
  META_DIR=$ROOT_META_DIR
elif [ -d "$CONF_META_DIR" ]; then
  echo "[存在] $CONF_META_DIR"
  META_DIR=$CONF_META_DIR
else
  # 如果没有找到任何元数据目录，创建一个
  echo "未找到任何元数据目录，将创建默认目录: $API_META_DIR"
  mkdir -p "$API_META_DIR"
  META_DIR=$API_META_DIR
fi

# 创建版本目录
VERSION_DIR="$META_DIR/DDP-1.2.1"
if [ ! -d "$VERSION_DIR" ]; then
  echo "创建版本目录: $VERSION_DIR"
  mkdir -p "$VERSION_DIR"
fi

# 创建服务目录
SERVICES_DIR="$VERSION_DIR/services"
if [ ! -d "$SERVICES_DIR" ]; then
  echo "创建服务目录: $SERVICES_DIR"
  mkdir -p "$SERVICES_DIR"
fi

# 检查环境变量
echo
echo "当前元数据目录: $META_DIR"
echo
echo "为确保IDEA能正确找到元数据目录，建议设置环境变量 META_BASE_DIR"
echo
echo "您可以通过以下命令临时设置环境变量:"
echo "export META_BASE_DIR=$META_DIR"
echo
echo "或者添加到~/.bashrc或~/.zshrc文件中以永久生效。"
echo

# 提示用户输入是否设置临时环境变量
read -p "是否设置临时环境变量 META_BASE_DIR? (Y/N): " choice
if [[ $choice == "Y" || $choice == "y" ]]; then
  export META_BASE_DIR="$META_DIR"
  echo "已设置环境变量 META_BASE_DIR=$META_DIR"
  echo "注意：此设置仅在当前终端会话中有效"
  echo "如需永久生效，请将以下命令添加到您的~/.bashrc或~/.zshrc文件中：" 
  echo "export META_BASE_DIR=$META_DIR"
else
  echo "未设置环境变量，将使用默认路径"
fi

echo
echo "元数据目录检查完成！"
echo 