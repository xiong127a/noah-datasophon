#!/bin/bash

# 在这里替换为你的Redis Master和Worker节点的主机和端口（空格分隔）
REDIS_MASTERS="${RedisMasterAddr}"
REDIS_WORKERS="${RedisSlaveAddr}"
SH_DIR=`dirname $0`
COMMAND_CREATE_CLUSTER="echo yes | $SH_DIR/redis-cli --cluster create "
COMMAND_ADD_NODE="$SH_DIR/redis-cli --cluster add-node "

CONTROL_SCRIPT="/opt/datasophon/redis/control_redis.sh"

create_cluster_command() {
    local master_list="$1"
    local create_command="$COMMAND_CREATE_CLUSTER"
    for master in $master_list; do
        create_command+=" $master"
    done
    echo "$create_command"
}

add_node_command() {
    local new_master="$1"
    local new_worker="$2"
    local add_node_command="$COMMAND_ADD_NODE $new_master $new_worker --cluster-slave"
    echo "$add_node_command"
}

check_redis_nodes() {
    for node in "<#noparse>${REDIS_MASTERS[@]}</#noparse>" "<#noparse>${REDIS_WORKERS[@]}</#noparse>"; do
        host=$(echo "$node" | cut -d ":" -f 1)
        port=$(echo "$node" | cut -d ":" -f 2)

        echo "Checking Redis node $node..."

        if ! $SH_DIR/redis-cli -h "$host" -p "$port" ping | grep -q "PONG"; then
            echo "Redis node $node is not running."
            return 1
        fi
    done

    echo "All Redis nodes are running."
    return 0
}

# 修改检查集群是否存在的函数
check_cluster_exists() {
    local node="$1"
    local host=$(echo "$node" | cut -d ":" -f 1)
    local port=$(echo "$node" | cut -d ":" -f 2)
    
    # 检查集群是否有槽位分配和已知节点数量
    local slots_assigned=$($SH_DIR/redis-cli -h "$host" -p "$port" cluster info | grep "cluster_slots_assigned:" | cut -d ":" -f2 | tr -d '[:space:]')
    local known_nodes=$($SH_DIR/redis-cli -h "$host" -p "$port" cluster info | grep "cluster_known_nodes:" | cut -d ":" -f2 | tr -d '[:space:]')
    
    # 如果没有槽位分配且只有一个节点，认为是新集群
    if [ "$slots_assigned" = "0" ] && [ "$known_nodes" = "1" ]; then
        echo "Redis cluster exists but not configured (no slots assigned). Will create new cluster."
        return 1  # 返回非零值，表示集群不存在或未配置
    fi
    
    # 如果有槽位分配或多个节点，认为集群已存在
    echo "A Redis cluster configuration already exists."
    return 0  # 返回零值，表示集群已存在
}

# 添加检查集群状态的函数
check_cluster_status() {
    local node="$1"
    local host=$(echo "$node" | cut -d ":" -f 1)
    local port=$(echo "$node" | cut -d ":" -f 2)
    
    # 获取集群状态
    local cluster_state=$($SH_DIR/redis-cli -h "$host" -p "$port" cluster info | grep "cluster_state:" | cut -d ":" -f2 | tr -d '[:space:]')
    
    echo "Current cluster state: $cluster_state"
    
    if [ "$cluster_state" = "ok" ]; then
        echo "Redis cluster is healthy."
        return 0  # 集群状态正常
    else
        echo "Redis cluster is in an unhealthy state: $cluster_state"
        return 1  # 集群状态异常
    fi
}

# 将REDIS_MASTERS和REDIS_WORKERS转换为数组
IFS=' ' read -ra MASTER_NODES <<< "$REDIS_MASTERS"
IFS=' ' read -ra WORKER_NODES <<< "$REDIS_WORKERS"

main() {
    if check_redis_nodes; then
        # 检查第一个主节点是否已经是集群的一部分
        FIRST_MASTER="<#noparse>${MASTER_NODES[0]}</#noparse>"
        
        if check_cluster_exists "$FIRST_MASTER"; then
            echo "Redis cluster already exists. Checking cluster status..."
            
            if check_cluster_status "$FIRST_MASTER"; then
                echo "Existing Redis cluster is healthy. No action needed."
                return 0
            else
                echo "WARNING: Existing Redis cluster has issues. Manual intervention may be required."
                return 1
            fi
        fi
        
        # 如果集群不存在，执行创建集群命令
        CREATE_CLUSTER_COMMAND=$(create_cluster_command "$REDIS_MASTERS")
        echo "Executing command: $CREATE_CLUSTER_COMMAND"
        eval "$CREATE_CLUSTER_COMMAND"

        # 检查上一条命令执行状态
        if [ $? -eq 0 ]; then
            echo "Create cluster command executed successfully."
        else
            echo "Error: Create cluster command failed."
            return 1
        fi

        # 执行添加节点命令
        FIRST_MASTER="<#noparse>${MASTER_NODES[0]}</#noparse>"
        echo $FIRST_MASTER
        for worker in "<#noparse>${WORKER_NODES[@]}</#noparse>"; do
            ADD_NODE_COMMAND=$(add_node_command "$worker" "$FIRST_MASTER")
            echo "Executing command: $ADD_NODE_COMMAND"
            eval "$ADD_NODE_COMMAND"

            # 检查上一条命令执行状态
            if [ $? -eq 0 ]; then
                echo "Add node command executed successfully."
            else
                echo "Error: Add node command failed."
                return 1
            fi
        done
    else
        echo "Not all Redis nodes are running. Cluster commands will not be executed."
    fi
}


# 执行主函数
main
