#!/bin/bash

# 在这里替换为你的Redis Master和Worker节点的主机和端口（空格分隔）
REDIS_MASTERS="${RedisMasterAddr}"
REDIS_WORKERS="${RedisSlaveAddr}"
COMMAND_CREATE_CLUSTER="echo yes | /opt/datasophon/redis/redis-cli --cluster create "
COMMAND_ADD_NODE="/opt/datasophon/redis/redis-cli --cluster add-node "

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

        if ! /opt/datasophon/redis/redis-cli -h "$host" -p "$port" ping | grep -q "PONG"; then
            echo "Redis node $node is not running."
            return 1
        fi
    done

    echo "All Redis nodes are running."
    return 0
}
# 将REDIS_MASTERS和REDIS_WORKERS转换为数组
IFS=' ' read -ra MASTER_NODES <<< "$REDIS_MASTERS"
IFS=' ' read -ra WORKER_NODES <<< "$REDIS_WORKERS"
main() {
    if check_redis_nodes; then
        # 如果所有节点都正常启动，执行创建集群命令
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
