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
      width: "10%",  // 减小操作系统列宽度，从15%改为10%
      customRender: (text, row) => {
        const h = vm.$createElement;

        // 检查是否为SSH错误
        const hasSSHError = vm.checkStatus(row.sshConnectStatus, 'error') || 
                            row.hasSSHError === true;
        // 检查是否为OS错误
        const hasOSError = vm.checkStatus(row.osInfoStatus, 'error') || 
                           vm.checkStatus(row.osStatus, 'error');
        // 检查是否正在加载
        const isLoading = row.sshConnectStatus === null || 
                         row.osInfo === null || row.osInfoStatus === null ||
                         vm.checkStatus(row.osStatus, 'loading') || 
                         vm.checkStatus(row.osStatus, 'pending');

        // SSH错误 - 显示错误状态和错误信息
        if (hasSSHError) {
          return h('a-tooltip', {
            props: {
              placement: 'right',
              arrowPointAtCenter: true,
              overlayStyle: { maxWidth: '350px' }
            }
          }, [
            h('span', { slot: 'title' }, [
              h('div', { class: 'ssh-error-details' }, [
                h('div', { class: 'ssh-error-title' }, ['SSH连接失败']),
                h('div', { class: 'ssh-error-message' }, [
                  row.sshErrorMsg || row.errorMessage
                ]),
                // 解析结构化错误信息
                vm.parseSSHErrorMessage(row.sshErrorMsg || row.errorMessage)
              ])
            ]),
            h('div', {
              style: {
                display: 'flex',
                alignItems: 'center',
                color: '#FF3B30'
              }
            }, [
              h('a-icon', {
                props: { type: 'warning' },
                style: { marginRight: '6px' }
              }),
              '获取失败'
            ])
          ]);
        }

        // 操作系统错误 - 显示OS获取失败的信息
        if (hasOSError) {
          return h('a-tooltip', {
            props: {
              placement: 'right',
              arrowPointAtCenter: true,
              overlayStyle: { maxWidth: '350px' }
            }
          }, [
            h('span', { slot: 'title' }, [
              h('div', { class: 'ssh-error-details' }, [
                h('div', { class: 'ssh-error-title' }, ['操作系统信息获取失败']),
                h('div', { class: 'ssh-error-message' }, [
                  row.osErrorMsg || '无法获取操作系统信息，请检查系统配置'
                ])
              ])
            ]),
            h('div', {
              style: {
                display: 'flex',
                alignItems: 'center',
                color: '#FF8800' // 使用橙色区分SSH错误和OS错误
              }
            }, [
              h('a-icon', {
                props: { type: 'warning' },
                style: { marginRight: '6px' }
              }),
              '获取失败'
            ])
          ]);
        }

        // 加载状态 - 显示加载中动画
        if (isLoading) {
          // 苹果风格的骨架屏加载动画
          return h('a-tooltip', {
            props: {
              placement: 'right',
              arrowPointAtCenter: true,
              overlayClassName: 'os-tooltip',
              getPopupContainer: () => document.body
            }
          }, [
            // 加载中浮窗内容
            h('span', {
              slot: 'title',
              class: 'os-detail-tooltip'
            }, [
              h('div', { class: 'os-detail-loading' }, [
                h('div', { class: 'os-detail-loading-header' }),
                h('div', { class: 'os-detail-loading-content' }, [
                  h('div', { class: 'os-detail-loading-line short' }),
                  h('div', { class: 'os-detail-loading-line medium' }),
                  h('div', { class: 'os-detail-loading-line' }),
                  h('div', { class: 'os-detail-loading-line short' }),
                  h('div', { class: 'os-detail-loading-line medium' })
                ]),
                h('div', {
                  class: 'os-detail-loading-text',
                  style: {
                    fontSize: '14px',
                    textAlign: 'center',
                    color: '#007AFF',
                    marginTop: '12px',
                    fontWeight: '500'
                  }
                }, ['正在优雅地检索操作系统信息...'])
              ])
            ]),

            // 显示的加载内容
            h('div', { class: 'os-loading-container' }, [
              // 背景滑动效果
              h('div', { class: 'os-loading-shine' }),
              // 内容区域
              h('div', { class: 'os-loading-content' }, [
                // 旋转的加载图标
                h('div', { class: 'os-loading-spinner' }),
                // 加载中文字
                h('span', { class: 'os-loading-text' }, ['获取系统信息'])
              ])
            ])
          ]);
        }

        // 使用osInfo中的数据
        const hasOsInfo = row.osInfo && (row.osInfo.distribution || row.osInfo.displayName);
        
        // 只使用displayName字段，不使用distributionName和distribution
        const osDisplayName = hasOsInfo 
          ? (row.osInfo.displayName || row.osInfo.distribution || '-')
          : (text || row.osType || '-');
          
        const osVersion = hasOsInfo ? row.osInfo.versionId : (row.osVersion || '');

        // 获取操作系统对应的图标路径
        function getOsIconPath(osInfo) {
          try {
            if (!osInfo) return require('@/assets/img/os-logos/linux-tux.svg');
            
            // 根据osInfo.distributionType或distributionId判断操作系统类型
            const distType = (osInfo.distributionType || '').toLowerCase();
            const distId = (osInfo.distributionId || '').toLowerCase();
            const distName = (osInfo.distribution || '').toLowerCase();
            
            // 确定主操作系统类型
            let osType = 'linux';
            
            if (distType === 'centos' || distId === 'centos' || distName.includes('centos')) {
              osType = 'centos';
            } else if (distType === 'ubuntu' || distId === 'ubuntu' || distName.includes('ubuntu')) {
              osType = 'ubuntu';
            } else if (distType === 'debian' || distId === 'debian' || distName.includes('debian')) {
              osType = 'debian';
            } else if (distType === 'redhat' || distId === 'redhat' || distName.includes('redhat') || distName.includes('red hat')) {
              osType = 'redhat';
            } else if (distType === 'kylin' || distId === 'kylin' || distName.includes('kylin') || distName.includes('麒麟')) {
              osType = 'kylin';
            } else if (distType === 'alpine' || distId === 'alpine' || distName.includes('alpine')) {
              osType = 'alpine';
            }
            
            // 使用switch语句根据操作系统类型返回对应图标
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
          } catch (error) {
            // 如果找不到图标文件，返回内置的数据URI
            return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0ibm9uZSI+PHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4IiByeD0iOCIgZmlsbD0iI2YwZjBmMCIvPjxwYXRoIGQ9Ik0yMy41IDE0QzIzLjUgMTIuMzQzMSAyNC44NDMxIDExIDI2LjUgMTFDMjguMTU2OSAxMSAyOS41IDEyLjM0MzEgMjkuNSAxNFYxNy42NzY4QzMwLjQ5MzcgMTguMTA3MiAzMS4zNjc0IDE4Ljc4NTUgMzIgMTkuNjMyVjE0QzMyIDExLjIzODYgMjkuNzYxNCA5IDI3IDlDMjQuMjM4NiA5IDIyIDExLjIzODYgMjIgMTRWMTkuNjM0QzIyLjYzMzEgMTguNzg2MSAyMy41MDc0IDE4LjEwNzEgMjQuNSAxNy42NzZWMTRIMjMuNVoiIGZpbGw9IiM1MjUyNTIiLz48cGF0aCBkPSJNMzEuOTk5OCAyOC45OUMzMi4wMDE4IDI5LjYzODkgMzEuODA3MSAzMC4yNzMzIDMxLjQ0MjkgMzAuODAyQzMxLjA3ODYgMzEuMzMwNyAzMC41NjAyIDMxLjczMDUgMjkuOTU5OCAzMS45NVYzNC43MkMzMi45MDc1IDM0LjEyMTMgMzUuMTAyIDMxLjM5NjYgMzUgMjguMjlDMzQuODk3OSAyNS4xODM0IDMyLjU1OTYgMjIuNjM5MiAyOS41IDIyLjI1VjE5LjI4QzI5LjUgMTkuMjggMzggMjEuMjggMzggMjlDMzggMzYuNzIgMjkuNTUgMzggMjkuNTUgMzhIMTkuMDNDMTkuMDMgMzggMTAuNTIgMzcuMjkgMTAuMDIgMjcuNzhDOS42OCAxOS43OSAxOS41IDE4LjI3IDE5LjUgMTguMjdWMjEuMjdDMTkuNSAyMS4yNyAxMy4wMDk4IDIyLjYxIDE0LjAyIDE5QzE1LjUgMTQgMjQuOTk5OCAxNCAyNC45OTk4IDE0QzI0Ljk5OTggMTQgMjYuOTk5OCAxNCAyOS4wMDA3IDE0Ljk5QzI5LjAwMDcgMTQuOTkgMjguOTUxNCAxNi42OTMxIDI4LjAyMDcgMTcuODJDMjUuNjgwNyAxOC40OSAyMyAyMC41MSAyMyAyNC41QzIzIDI5LjE1IDI3LjAwMDIgMzAuMTcgMjcuMDAwMiAzMS4yNVYzNC42NkMyMi42NDczIDM0LjMzMDMgMTkuMTk5MSAzMC42NjAzIDE5LjAxOTggMjYuMDZDMTkuMDE5OCAyNS44NiAxOS4wMTk4IDI1LjY2IDE5LjAxOTggMjUuNDZDMTkuMDE5OCAyMy42OTQ1IDE5LjYzOTQgMjEuOTkxMiAyMC43Mzk3IDIwLjY4MTdDMjEuODQwMSAxOS4zNzIyIDIzLjM0NDIgMTguNTUxNiAyNC45OTk4IDE4LjQyVjIyLjE5QzIzLjI4MTQgMjIuNDA1NiAyMS44NTA1IDIzLjU0MTMgMjEuMzcwOSAyNS4xN0MyMi4yMTc3IDI3LjY0MjcgMjQuNzY1OCAyOS4xMjI0IDI3LjI3MDcgMjguNThDMjcuNzk5OSAyOC40NiAyOC4zMTYyIDI4LjI5MTQgMjguODE4MyAyOC4wOEMyOS4wNTQ5IDI3Ljk4ODYgMjkuMzE2MyAyNy45OTk3IDI5LjU0OCAyOC4xMTFDMjkuNzc5NyAyOC4yMjIzIDI5Ljk2MDIgMjguNDI1MiAzMC4wNCAyOC42OEMzMC4yMSAyOS4xNTcgMzAuMzI0NCAyOS42NTMzIDMwLjM4MTcgMzAuMTU3M0MzMC41MDUzIDI5LjY3MjggMzAuNTA4NSAyOS4xNTgxIDMwLjM5MDkgMjguNjcyQzMwLjM5MDkgMjguNjcyIDMxLjk5OTggMjguOTkgMzEuOTk5OCAyOC45OVoiIGZpbGw9IiM1MjUyNTIiLz48L3N2Zz4=';
          }
        }

        const iconPath = getOsIconPath(hasOsInfo ? row.osInfo : null);

        // 返回操作系统信息显示
        if (hasOsInfo) {
          return h('a-tooltip', {
            props: {
              placement: 'right',
              arrowPointAtCenter: true,
              overlayClassName: 'os-tooltip',
              getPopupContainer: () => document.body
            }
          }, [
            // 使用新的操作系统浮窗组件
            h('span', {
              slot: 'title',
              class: 'os-detail-tooltip'
            }, [
              h('OsFloatingCard', {
                props: {
                  osInfo: row.osInfo,
                  cpuStatus: row.cpuStatus || 'pending',
                  memoryStatus: row.memoryStatus || 'pending',
                  diskStatus: row.diskStatus || 'pending',
                  swapStatus: row.swapStatus || 'pending',
                  gpuStatus: row.gpuStatus || 'pending'
                }
              })
            ]),

            // 显示的操作系统信息
            h('div', {
              style: {
                display: 'flex',
                alignItems: 'center'
              }
            }, [
              // 操作系统图标
              h('div', {
                style: {
                  width: '24px',
                  height: '24px',
                  marginRight: '8px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }
              }, [
                h('img', {
                  attrs: {
                    src: iconPath,
                    alt: osDisplayName
                  },
                  style: {
                    width: '20px',
                    height: '20px'
                  }
                })
              ]),

              // 操作系统名称和版本
              h('div', {
                style: {
                  display: 'flex',
                  flexDirection: 'column'
                }
              }, [
                h('span', {
                  style: {
                    color: '#1D1D1F',
                    fontWeight: '500',
                    fontSize: '13px',
                    lineHeight: '1.3'
                  }
                }, [osDisplayName]),
                osVersion ? h('span', {
                  style: {
                    color: '#8E8E93',
                    fontSize: '11px',
                    lineHeight: '1.3'
                  }
                }, [osVersion]) : null
              ])
            ])
          ]);
        }

        // 当没有有效的osInfo时，显示简单的信息
        return h('div', {
          style: {
            display: 'flex',
            alignItems: 'center'
          }
        }, [
          h('span', {
            style: {
              color: '#8E8E93',
              fontSize: '13px'
            }
          }, [text || '未知操作系统'])
        ]);
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
      title: "状态",
      key: "status",
      width: "15%",  // 增加状态列宽度
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
          MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
        };

        // 使用主机的状态
        const hostStatus = row.statusStr || row.status || '';

        // 如果主机有状态，直接显示
        if (hostStatus && statusMap[hostStatus]) {
          const status = statusMap[hostStatus];
          return h('span', {
            class: 'flex-container',
            style: {
              display: 'flex',
              alignItems: 'center',
              color: status.color
            }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [status.text])
          ]);
        }

        return h('span', {}, ['-']);
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
          MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
        };

        // 检查主机是否有检查项
        const checkItems = row.checkItems || [];

        // 优先级处理：检查中 > 待检查 > 失败 > 跳过 > 成功

        // 1. 先检查是否有正在检查中的项目
        const currentItem = checkItems.find(item => item.status === 'CHECKING');
        if (currentItem) {
          const status = statusMap[currentItem.status];
          return h('span', {
            class: 'flex-container',
            style: { display: 'flex', alignItems: 'center', color: status.color }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [currentItem.itemName])
          ]);
        }

        // 2. 其次检查是否有待检查的项目
        const waitingItem = checkItems.find(item => item.status === 'WAITING');
        if (waitingItem) {
          const status = statusMap[waitingItem.status];
          return h('span', {
            class: 'flex-container',
            style: { display: 'flex', alignItems: 'center', color: status.color }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [waitingItem.itemName])
          ]);
        }

        // 3. 查找失败的项目，并显示最后一个失败项
        const failedItems = checkItems.filter(item => item.status === 'FAILED');
        if (failedItems.length > 0) {
          const lastFailedItem = failedItems[failedItems.length - 1];
          const status = statusMap[lastFailedItem.status];
          return h('span', {
            class: 'flex-container',
            style: { display: 'flex', alignItems: 'center', color: status.color }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [lastFailedItem.itemName])
          ]);
        }

        // 4. 查找跳过的项目，显示最后一个跳过项
        const skippedItems = checkItems.filter(item => item.status === 'SKIPPED');
        if (skippedItems.length > 0) {
          const lastSkippedItem = skippedItems[skippedItems.length - 1];
          const status = statusMap[lastSkippedItem.status];
          return h('span', {
            class: 'flex-container',
            style: { display: 'flex', alignItems: 'center', color: status.color }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [lastSkippedItem.itemName])
          ]);
        }

        // 5. 最后查找成功的项目，显示最后一个成功项
        const successItems = checkItems.filter(item => item.status === 'SUCCESS');
        if (successItems.length > 0) {
          const lastSuccessItem = successItems[successItems.length - 1];
          const status = statusMap[lastSuccessItem.status];
          return h('span', {
            class: 'flex-container',
            style: { display: 'flex', alignItems: 'center', color: status.color }
          }, [
            h('a-icon', {
              props: {
                type: status.icon,
                theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                twoToneColor: status.color,
                spin: status.icon === 'loading'
              },
              style: { fontSize: '14px', marginRight: '4px' }
            }),
            h('span', {}, [lastSuccessItem.itemName])
          ]);
        }

        // 如果没有任何检查项，则显示占位符
        return h('span', {}, ['-']);
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