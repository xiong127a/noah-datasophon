// 表格列定义
export default function createColumns(vm) {
  return [
    {
      title: "序号",
      key: "index",
      width: 50,
      customRender: (text, row, index) => {
        const h = vm.$createElement;
        const displayIndex = parseInt(
            vm.pagination.current === 1
                ? index + 1
                : index + 1 + vm.pagination.pageSize * (vm.pagination.current - 1)
        );
        return h('span', {}, [displayIndex]);
      },
    },
    {
      title: "主机名",
      key: "hostname",
      dataIndex: "hostname",
      width: 200,
      customRender: (text, record) => {
        const h = vm.$createElement;
        return h('div', { class: 'hostname-column' }, [
          // 添加主机名悬浮卡片
          h('a-tooltip', {
            props: {
              placement: 'right',
              arrowPointAtCenter: true,
              overlayClassName: 'hostname-tooltip',
              getPopupContainer: () => document.body
            }
          }, [
            // 悬浮内容 - SSH错误时显示错误信息
            h('span', {
              slot: 'title',
              class: 'hostname-detail-tooltip'
            }, [
              record.hasSSHError || record.sshConnectStatus === 'error' ? 
              // SSH错误时显示的内容
              h('div', { class: 'ssh-error-card' }, [
                h('div', { class: 'ssh-error-header' }, [
                  h('div', { class: 'ssh-error-icon' }, [
                    h('a-icon', { props: { type: 'warning', theme: 'filled' }, style: { color: '#FF3B30', fontSize: '20px' } })
                  ]),
                  h('div', { class: 'ssh-error-title' }, ['SSH连接失败'])
                ]),
                h('div', { class: 'ssh-error-content' }, [
                  h('div', { class: 'ssh-error-message' }, [
                    record.sshErrorMsg || record.errorMessage
                  ]),
                  // 解析结构化错误信息
                  vm.parseSSHErrorMessage(record.sshErrorMsg || record.errorMessage)
                ])
              ]) :
              // 正常情况下显示常规主机信息卡片
              h('HostnameFloatingCard', {
                props: {
                  hostInfo: record
                }
              })
            ]),
            
            // 显示的主机名文本 - 突出显示SSH错误状态
            h('div', { 
              class: 'hostname-display', 
              style: record.hasSSHError || record.sshConnectStatus === 'error' ? 
                { display: 'flex', alignItems: 'center' } : {}
            }, [
              // 如果有SSH错误，显示错误图标
              record.hasSSHError || record.sshConnectStatus === 'error' ? 
              h('a-icon', { 
                props: { type: 'warning' }, 
                style: { color: '#FF3B30', marginRight: '6px' } 
              }) : null,
              
              // 主机名或未知主机
              record.hasSSHError || record.sshConnectStatus === 'error' ?
              h('span', { 
                class: 'hostname-text error', 
                style: { color: '#FF3B30' },
                title: '获取主机名失败 (SSH连接错误)' 
              }, [record.hostname || '未获取到主机名']) :
              h('span', { 
                class: 'hostname-text', 
                title: record.fqdn || record.hostname || '主机名加载中' 
              }, [
                record.hostname || h('div', { class: 'hostname-loading-container' }, [
                  h('div', { class: 'hostname-loading-dots' }, [
                    h('span', { class: 'hostname-loading-dot' }),
                    h('span', { class: 'hostname-loading-dot' }),
                    h('span', { class: 'hostname-loading-dot' })
                  ]),
                  h('span', { class: 'hostname-loading-text' }, ['获取主机名'])
                ])
              ])
            ]),
          ]),
          
          // 编辑图标
          h('a-tooltip', { props: { title: '编辑主机名' } }, [
            h('a-icon', {
              class: 'hostname-edit-icon',
              props: { type: 'edit' },
              on: {
                click: (e) => {
                  e.stopPropagation();
                  vm.editHostname(record);
                }
              }
            })
          ])
        ]);
      }
    },
    {
      title: "主机IP",
      key: "ip",
      dataIndex: "ip",
      width: 130,
      customRender: (text) => {
        const h = vm.$createElement;
        return h('span', {
          style: {
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis'
          }
        }, [text]);
      }
    },
    {
      title: "操作系统",
      key: "osType",
      dataIndex: "osType",
      width: "160px",
      customRender: (text, row) => {
        let osDisplayName = text || '';
        const { osInfo } = row;
        
        // 如果有osInfo对象，从中获取操作系统名称
        if (osInfo) {
          // 直接使用distribution字段，不拼接版本号
          if (osInfo.distribution) {
            osDisplayName = osInfo.distribution;
            // 移除版本号拼接
          }
        }
        
        return osDisplayName || (text || 'Linux');
      }
    },
    {
      title: "当前受管",
      key: "managed",
      dataIndex: "managed",
      width: "90px", // 添加固定宽度
      customRender: (text, row, index) => {
        const h = vm.$createElement;
        return h('div', {
          style: {
            display: 'inline-flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            fontSize: '13px',
            fontWeight: '500',
            backgroundColor: text ? 'rgba(52, 199, 89, 0.1)' : 'rgba(142, 142, 147, 0.1)',
            color: text ? '#34c759' : '#8e8e93',
            transition: 'all 0.3s ease'
          }
        }, [
          h('span', {
            style: {
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: text ? '#34c759' : '#8e8e93',
              marginRight: '6px',
              display: 'inline-block'
            }
          }),
          text ? "是" : "否"
        ]);
      },
    },
    {
      title: "状态test",
      key: "status",
      width: "10%",  // 增加状态列宽度
      customRender: (text, row) => {
        const h = vm.$createElement;

        // 状态映射
        const statusMap = {
          CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
          WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
          SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
          FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
          SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
          TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
          MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' },
          FIXING: { text: '修复中', color: '#5856D6', icon: 'tool', spin: true },
          WAITING_FIX: { text: '等待修复', color: '#FF9F0A', icon: 'hourglass' }
        };

        // 创建居中包装容器
        const createStatusElement = (status) => {
          return h('div', {
            style: {
              display: 'flex',
              justifyContent: 'center', // 水平居中
              alignItems: 'center',     // 垂直居中
              width: '100%'
            }
          }, [
            h('span', {
              class: 'status-tag',
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '12px',
                backgroundColor: `${status.color}15`, // 15是透明度，相当于rgba的0.1
                border: `1px solid ${status.color}30`, // 30是透明度，相当于rgba的0.2
                fontSize: '13px',
                color: status.color
              }
            }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  theme: !['loading', 'clock-circle', 'tool', 'hourglass'].includes(status.icon) ? "twoTone" : undefined,
                  twoToneColor: status.color,
                  spin: status.spin || status.icon === 'loading' || status.icon === 'tool'
                },
                style: { fontSize: '14px', marginRight: '4px' }
              }),
              h('span', {}, [status.text])
            ])
          ]);
        };
        
        // 创建显示自定义文本的函数
        const createCustomStatusElement = (status, customText) => {
          return h('div', {
            style: {
              display: 'flex',
              justifyContent: 'center', // 水平居中
              alignItems: 'center',     // 垂直居中
              width: '100%'
            }
          }, [
            h('span', {
              class: 'custom-status-tag',
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '12px',
                backgroundColor: `${status.color}15`, // 15是透明度，相当于rgba的0.1
                border: `1px solid ${status.color}30`, // 30是透明度，相当于rgba的0.2
                fontSize: '13px',
                color: status.color
              }
            }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  theme: !['loading', 'clock-circle', 'tool', 'hourglass'].includes(status.icon) ? "twoTone" : undefined,
                  twoToneColor: status.color,
                  spin: status.spin || status.icon === 'loading' || status.icon === 'tool'
                },
                style: { fontSize: '14px', marginRight: '4px' }
              }),
              h('span', {}, [customText || status.text])
            ])
          ]);
        };

        // 检查项状态优先级：修复中 > 等待修复 > 检查中 > 等待检查 > 失败 > 跳过 > 成功
        const priorityOrder = ['FIXING', 'WAITING_FIX', 'CHECKING', 'WAITING', 'FAILED', 'SKIPPED', 'SUCCESS'];
        
        // 优先检查主机是否有显式的修复中状态
        if (row.status === 'FIXING' || row.statusStr === 'FIXING') {
          const status = statusMap['FIXING'];
          return createStatusElement(status);
        }
        
        // 优先检查主机是否有显式的等待修复状态
        if (row.status === 'WAITING_FIX' || row.statusStr === 'WAITING_FIX') {
          const status = statusMap['WAITING_FIX'];
          return createStatusElement(status);
        }

        // 检查检查项状态，按优先级顺序确定主机的显示状态
        if (row.checkItems && row.checkItems.length > 0) {
          // 遍历优先级顺序，找到最高优先级的检查项状态
          for (const statusType of priorityOrder) {
            const hasItemWithStatus = row.checkItems.some(item => item.status === statusType);
            if (hasItemWithStatus) {
              const status = statusMap[statusType];
              return createStatusElement(status);
            }
          }
        }

        // 如果主机有状态且在状态映射中，则显示主机状态
        const hostStatus = row.statusStr || row.status || '';
        if (hostStatus && statusMap[hostStatus]) {
          const status = statusMap[hostStatus];
          return createStatusElement(status);
        }

        // 默认显示等待检查
        const defaultStatus = statusMap['WAITING'];
        return createStatusElement(defaultStatus);
      },
    },
    {
      title: "检查项",
      key: "checkItem",
      width: "18%",  // 增加检查项列宽度
      customRender: (text, row) => {
        const h = vm.$createElement;

        // 状态映射
        const statusMap = {
          CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
          WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
          SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
          FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
          SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
          TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
          MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' },
          FIXING: { text: '修复中', color: '#5856D6', icon: 'tool', spin: true },
          WAITING_FIX: { text: '等待修复', color: '#FF9F0A', icon: 'hourglass' }
        };

        // 创建居中包装容器(在检查项列也需要定义此函数)
        const createStatusElement = (status) => {
          return h('div', {
            style: {
              display: 'flex',
              justifyContent: 'center', // 水平居中
              alignItems: 'center',     // 垂直居中
              width: '100%'
            }
          }, [
            h('span', {
              class: 'status-tag',
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '12px',
                backgroundColor: `${status.color}15`, // 15是透明度，相当于rgba的0.1
                border: `1px solid ${status.color}30`, // 30是透明度，相当于rgba的0.2
                fontSize: '13px',
                color: status.color
              }
            }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  theme: !['loading', 'clock-circle', 'tool', 'hourglass'].includes(status.icon) ? "twoTone" : undefined,
                  twoToneColor: status.color,
                  spin: status.spin || status.icon === 'loading' || status.icon === 'tool'
                },
                style: { fontSize: '14px', marginRight: '4px' }
              }),
              h('span', {}, [status.text])
            ])
          ]);
        };
        
        // 创建显示自定义文本的函数
        const createCustomStatusElement = (status, customText) => {
          return h('div', {
            style: {
              display: 'flex',
              justifyContent: 'center', // 水平居中
              alignItems: 'center',     // 垂直居中
              width: '100%'
            }
          }, [
            h('span', {
              class: 'custom-status-tag',
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '12px',
                backgroundColor: `${status.color}15`, // 15是透明度，相当于rgba的0.1
                border: `1px solid ${status.color}30`, // 30是透明度，相当于rgba的0.2
                fontSize: '13px',
                color: status.color
              }
            }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  theme: !['loading', 'clock-circle', 'tool', 'hourglass'].includes(status.icon) ? "twoTone" : undefined,
                  twoToneColor: status.color,
                  spin: status.spin || status.icon === 'loading' || status.icon === 'tool'
                },
                style: { fontSize: '14px', marginRight: '4px' }
              }),
              h('span', {}, [customText || status.text])
            ])
          ]);
        };

        // 检查主机是否有检查项
        const checkItems = row.checkItems || [];
        if (checkItems.length === 0) {
          // 如果没有任何检查项，则显示等待检查
          const defaultStatus = statusMap['WAITING'];
          return createStatusElement(defaultStatus);
        }

        // 优先级顺序：修复中 > 等待修复 > 检查中 > 等待检查 > 失败 > 跳过 > 成功
        // 按优先级定义状态类型的顺序
        const priorityOrder = ['FIXING', 'WAITING_FIX', 'CHECKING', 'WAITING', 'FAILED', 'SKIPPED', 'SUCCESS'];
        
        // 创建状态优先级映射，值越小优先级越高
        const priorityMap = {};
        priorityOrder.forEach((status, index) => {
          priorityMap[status] = index;
        });
        
        // 对检查项按优先级顺序排序
        const sortedItems = [...checkItems].sort((a, b) => {
          const priorityA = priorityMap[a.status] !== undefined ? priorityMap[a.status] : 999;
          const priorityB = priorityMap[b.status] !== undefined ? priorityMap[b.status] : 999;
          return priorityA - priorityB;
        });
        
        // 获取最高优先级的检查项
        const highestPriorityItem = sortedItems[0];
        
        // 确保检查项存在并且有状态
        if (highestPriorityItem && highestPriorityItem.status) {
          const status = statusMap[highestPriorityItem.status] || statusMap['WAITING'];
          const iconSpin = highestPriorityItem.status === 'CHECKING' || 
                           highestPriorityItem.status === 'FIXING' || 
                           highestPriorityItem.status === 'TERMINATING';
          
          return createCustomStatusElement(status, highestPriorityItem.itemName);
        }

        // 最后的默认情况
        const defaultStatus = statusMap['WAITING'];
        return createStatusElement(defaultStatus);
      },
    },
    {
      title: "操作",
      key: "action",
      width: "10%",
      customRender: (text, row) => {
        const h = vm.$createElement;
        const isChecking = row.status === 'CHECKING' || row.statusStr === 'CHECKING';
        const isWaiting = row.status === 'WAITING' || row.statusStr === 'WAITING';

        return h('div', { class: 'action-buttons apple-actions' }, [
          // 终止按钮 - 检查中时显示
          isChecking ? h('a-button', {
            attrs: {
              type: 'danger',
              size: 'small'
            },
            class: 'apple-button danger',
            on: {
              click: () => vm.stopCheck(row)
            }
          }, ["终止"]) : null,

          // 重试按钮 - 非检查中且非等待检查时显示
          !isChecking ? h('button', {
            props: {
              type: 'link',
              size: 'small',
            },
            style: {
              border: 'none',
              backgroundColor: 'rgba(0, 122, 255, 0.1)',
              color: '#007AFF',
              padding: '6px 12px',
              borderRadius: '12px',
              fontSize: '13px',
              fontWeight: '500',
              cursor: isWaiting ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              opacity: isWaiting ? '0.5' : '1',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            },
            attrs: {
              disabled: isWaiting // 等待检查时禁用，但不会显示禁用标识
            },
            on: {
              click: () => vm.retryEnvironment(row)
            }
          }, [
            h('a-icon', {
              props: { type: 'redo' },
              style: { marginRight: '4px', fontSize: '12px' }
            }),
            "重试"
          ]) : null
        ].filter(Boolean));
      },
    }
  ];
}

// 获取操作系统图标
const getOsIcon = (row) => {
  let osType = 'linux';
  if (row.osInfo) {
    if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('centos')) {
      osType = 'centos';
    } else if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('ubuntu')) {
      osType = 'ubuntu';
    } else if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('debian')) {
      osType = 'debian';
    } else if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('redhat')) {
      osType = 'redhat';
    } else if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('kylin')) {
      osType = 'kylin';
    } else if (row.osInfo.distribution && row.osInfo.distribution.toLowerCase().includes('alpine')) {
      osType = 'alpine';
    }
  }
  
  switch (osType) {
    case 'centos':
      return require('@/assets/img/os-logos/centos.svg');
    case 'ubuntu':
      return require('@/assets/img/os-logos/ubuntu.svg');
    case 'debian':
      return require('@/assets/img/os-logos/debian.svg');
    case 'redhat':
      return require('@/assets/img/os-logos/redhat.svg');
    case 'kylin':
      return require('@/assets/img/os-logos/kylin.png');
    case 'alpine':
      return require('@/assets/img/os-logos/alpine.svg');
    default:
      return require('@/assets/img/os-logos/linux-tux.svg');
  }
}