#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# 获取系统cpu、内存、磁盘信息脚本
# 查看逻辑CPU的个数
coreNum=`cat /proc/cpuinfo| grep "processor"| wc -l`
# cpu 15分钟平均负载
#averageLoad=`cat /proc/loadavg | awk '{print $3}'`
 
# 总内存大小GB
totalMem=`free -m | grep Mem | awk '{print $2/1024}'`
# 确保totalMem有值，如果为空设置为0
if [ -z "$totalMem" ]; then
  totalMem="0"
fi

#内存使用量GB
#usedMem=`free -m | grep Mem | awk '{print $3/1024}'`

#内存使用率
#memUsedPersent=`free -m | grep Mem | awk '{print $3/$2*100}'`

 
# 磁盘大小GB，排除tmpfs类型
totalDisk=`df -k | grep -v "tmpfs" | egrep -A 1 "mapper|sd" | awk 'NF>1{print $(NF-4)}' | awk -v used=0 '{used+=$1}END{printf "%.2f\n",used/1048576}'`
# 确保totalDisk有值，如果为空设置为0
if [ -z "$totalDisk" ]; then
  totalDisk="0.00"
fi
 
#usedDisk=`df -k | grep -v "tmpfs" | egrep -A 1 "mapper|sd" | awk 'NF>1{print $(NF-3)}' | awk -v used=0 '{used+=$1}END{printf "%.2f\n",used/1048576}'`
 
#diskAvail=`df -k | grep -v "tmpfs" | egrep -A 1 "mapper|sd" | awk 'NF>1{print $(NF-2)}' | awk -v used=0 '{used+=$1}END{printf "%.2f\n",used/1048576}'`

#diskUsedPersent=`awk 'BEGIN{printf "%.1f\n",('$usedDisk'/'$totalDisk')*100}'`


# 确保输出有效的JSON格式，即使变量为空
echo "{\"coreNum\": $coreNum, \"totalMem\": \"$totalMem\", \"totalDisk\": $totalDisk}"

