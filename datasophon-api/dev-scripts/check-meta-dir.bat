@echo off
REM 检查并创建元数据目录脚本

echo ===============================================
echo      Noah大数据平台元数据目录检查工具
echo ===============================================
echo.

REM 获取当前目录
set CURRENT_DIR=%cd%
echo 当前目录: %CURRENT_DIR%

REM 尝试确定项目根目录
cd %CURRENT_DIR%
if exist "datasophon-api" (
  echo 当前目录是项目根目录
  set PROJECT_ROOT=%CURRENT_DIR%
) else (
  REM 检查是否在子模块中
  cd ..
  if exist "datasophon-api" (
    echo 当前在子模块中，已找到项目根目录
    set PROJECT_ROOT=%cd%
  ) else (
    echo 无法确定项目根目录，请在项目根目录或其子模块中运行此脚本
    goto :end
  )
)

echo 项目根目录: %PROJECT_ROOT%
echo.

REM 设置可能的元数据目录路径
set API_META_DIR=%PROJECT_ROOT%\datasophon-api\src\main\resources\meta
set SERVICE_META_DIR=%PROJECT_ROOT%\datasophon-service\src\main\resources\meta
set ROOT_META_DIR=%PROJECT_ROOT%\meta
set CONF_META_DIR=%PROJECT_ROOT%\conf\meta

REM 检查所有可能的路径
echo 检查可能的元数据目录位置...

if exist "%API_META_DIR%" (
  echo [存在] %API_META_DIR%
  set META_DIR=%API_META_DIR%
  goto :create_version_dir
)

if exist "%SERVICE_META_DIR%" (
  echo [存在] %SERVICE_META_DIR%
  set META_DIR=%SERVICE_META_DIR%
  goto :create_version_dir
)

if exist "%ROOT_META_DIR%" (
  echo [存在] %ROOT_META_DIR%
  set META_DIR=%ROOT_META_DIR%
  goto :create_version_dir
)

if exist "%CONF_META_DIR%" (
  echo [存在] %CONF_META_DIR%
  set META_DIR=%CONF_META_DIR%
  goto :create_version_dir
)

REM 如果没有找到任何元数据目录，创建一个
echo 未找到任何元数据目录，将创建默认目录: %API_META_DIR%
mkdir "%API_META_DIR%"
set META_DIR=%API_META_DIR%

:create_version_dir
REM 创建版本目录
set VERSION_DIR=%META_DIR%\DDP-1.2.1
if not exist "%VERSION_DIR%" (
  echo 创建版本目录: %VERSION_DIR%
  mkdir "%VERSION_DIR%"
)

REM 创建服务目录
set SERVICES_DIR=%VERSION_DIR%\services
if not exist "%SERVICES_DIR%" (
  echo 创建服务目录: %SERVICES_DIR%
  mkdir "%SERVICES_DIR%"
)

REM 检查环境变量
echo.
echo 当前元数据目录: %META_DIR%
echo.
echo 为确保IDEA能正确找到元数据目录，建议设置环境变量 META_BASE_DIR
echo.
echo 您可以通过以下命令临时设置环境变量:
echo set META_BASE_DIR=%META_DIR%
echo.
echo 或者添加到系统环境变量中以永久生效。
echo.

REM 提示用户输入是否设置临时环境变量
set /p choice=是否设置临时环境变量 META_BASE_DIR? (Y/N): 
if /i "%choice%"=="Y" (
  setx META_BASE_DIR "%META_DIR%"
  echo 已设置环境变量 META_BASE_DIR=%META_DIR%
) else (
  echo 未设置环境变量，将使用默认路径
)

echo.
echo 元数据目录检查完成！
echo.

:end
pause 