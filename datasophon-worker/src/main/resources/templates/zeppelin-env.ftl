#!/bin/bash

export ZEPPELIN_ADDR=0.0.0.0
export ZEPPELIN_PORT=8889
parent_dir=$(dirname "$(cd "$(dirname "$0")" && pwd)")
export JAVA_HOME=$parent_dir/jdk1.8.0_311

<#list itemList as item>
export ${item.name}=${item.value}
</#list>