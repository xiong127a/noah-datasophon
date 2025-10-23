#!/bin/bash

echo "======================================="
echo "环境检查API诊断脚本"
echo "======================================="
echo ""

BASE_URL="http://localhost:8081/ddh/api/v1"

echo "1. 测试环境检查控制器是否正常工作..."
echo "   URL: $BASE_URL/environment-check/test"
curl -s "$BASE_URL/environment-check/test" | json_pp
echo ""
echo ""

echo "2. 测试主机校验控制器 (对比用)..."
echo "   URL: $BASE_URL/host-validation/test"
curl -s "$BASE_URL/host-validation/test" | json_pp
echo ""
echo ""

echo "3. 查看Actuator映射信息..."
echo "   URL: http://localhost:8081/ddh/actuator/mappings"
echo "   搜索environment-check相关的映射..."
curl -s "http://localhost:8081/ddh/actuator/mappings" | grep -i "environment-check" | head -n 20
echo ""
echo ""

echo "诊断脚本完成!"
echo "请检查上述输出，确认:"
echo "  1. environment-check/test 端点是否返回成功响应"
echo "  2. 映射信息中是否包含 /ddh/api/v1/environment-check/start"
echo ""

