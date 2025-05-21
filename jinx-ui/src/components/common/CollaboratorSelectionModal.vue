<template>
  <a-modal
    :visible="visible"
    title="选择协同人员"
    width="1000px"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="collaborator-selection">
      <!-- 中间人员列表 -->
      <div class="user-list-container">
        <a-card title="人员列表" class="user-card">
          <!-- 搜索和角色选择区域 -->
          <div class="filter-area">
            <!-- 角色下拉框 -->
            <div class="filter-item">
              <span class="label">角色：</span>
              <a-select
                v-model:value="selectedRoleId"
                placeholder="请选择角色"
                style="width: 200px;"
                :loading="roleLoading"
                @change="handleRoleChange"
                :options="roleOptions"
                allow-clear
              />
            </div>

            <!-- 搜索框 -->
            <div class="filter-item">
              <span class="label">搜索：</span>
              <a-input-search
                v-model:value="userSearchKeyword"
                placeholder="搜索人员姓名或工号"
                @search="handleUserSearch"
                style="width: 240px;"
                allow-clear
                enter-button
                :loading="userLoading"
              />
            </div>
          </div>

          <!-- 人员列表 -->
          <a-table
            :columns="userColumns"
            :data-source="userList"
            :loading="userLoading"
            :pagination="userPagination"
            row-key="id"
            :locale="{emptyText: getEmptyText()}"
            :row-selection="{
              selectedRowKeys: getSelectedRowKeys(),
              onChange: onUserSelect,
              preserveSelectedRowKeys: true  // 保留选中项
            }"
            @change="handleTableChange"
          >
          </a-table>
        </a-card>
      </div>

      <!-- 右侧已选人员列表 -->
      <div class="selected-users-container">
        <a-card title="已选人员" class="selected-users-card">
          <div class="selected-users-header">
            <span>已选择 {{ selectedUsers.length }} 人</span>
            <a-button type="link" @click="clearSelectedUsers">清空</a-button>
          </div>
          <div class="selected-users-list">
            <a-list
              :data-source="selectedUsers"
              :locale="{emptyText: '暂未选择人员'}"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <div class="selected-user-item">
                    <div class="user-info">
                      <a-avatar
                        :src="item.avatar || item.avatarUrl"
                        size="small"
                        style="margin-right: 8px; background-color: #87d068"
                      >
                        {{item.nickname ? item.nickname.charAt(0) : (item.username ? item.username.charAt(0) : '?')}}
                      </a-avatar>
                      <span class="user-name">{{ item.nickname || item.username }}</span>
                      <span class="user-dept">{{ getUserDepartmentDisplay(item) }}</span>
                    </div>
                    <a-button type="link" @click="removeSelectedUser(item)">移除</a-button>
                  </div>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-card>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="modal-footer">
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" @click="handleConfirm">确认</a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script>
import { defineComponent, ref, reactive, watch, onMounted, computed, h, resolveComponent } from 'vue';
import { message } from 'ant-design-vue';
import { getRoleList, getRoleUsers } from '@/api/role';
import { getUserDepartments } from '@/api/org';

export default defineComponent({
  name: 'CollaboratorSelectionModal',
  props: {
    visible: {
      type: Boolean,
      required: true
    }
  },
  emits: ['update:visible', 'cancel', 'confirm'],
  setup(props, { emit }) {
    // 数据
    const roleSearchKeyword = ref('');
    const userSearchKeyword = ref('');
    const roleList = ref([]);
    const userList = ref([]);
    const selectedRoleId = ref(null);
    const roleLoading = ref(false);
    const userLoading = ref(false);
    const selectedUserIds = ref([]);
    const selectedUsers = ref([]);

    // 分页配置
    const userPagination = ref({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 人`
    });

    // 角色选项列表
    const roleOptions = computed(() => {
      return roleList.value.map(role => ({
        label: `${role.roleName || ''} (${role.roleCode || ''})`,
        value: role.id
      }));
    });

    // 表格列定义
    const userColumns = [
      {
        title: '头像',
        dataIndex: 'avatar',
        key: 'avatar',
        width: '60px',
        customRender: ({ text, record }) => {
          // 使用用户名首字母作为默认显示
          const firstChar = record.nickname ? record.nickname.charAt(0) : (record.username ? record.username.charAt(0) : '?');
          // 获取头像URL
          const avatarUrl = record.avatar || record.avatarUrl;

          return {
            children: h(resolveComponent('a-avatar'), {
              src: avatarUrl,
              size: 'small',
              style: 'background-color: #87d068'
            }, () => firstChar)
          };
        }
      },
      {
        title: '工号',
        dataIndex: 'username',
        key: 'username',
        width: '15%'
      },
      {
        title: '姓名',
        dataIndex: 'nickname',
        key: 'nickname',
        width: '15%'
      },
      {
        title: '部门',
        dataIndex: 'computedDepartment',
        key: 'department',
        ellipsis: true,
        customRender: ({ text, record }) => {
          console.log(`表格渲染部门, 用户: ${record.nickname}, 部门数据:`, {
            computedDepartment: record.computedDepartment,
            departmentName: record.departmentName,
            departments: record.departments
          });

          // 首先检查是否有计算好的部门名称
          if (record.computedDepartment) {
            return record.computedDepartment;
          }

          // 尝试从departments数组中获取
          if (record.departments && record.departments.length > 0) {
            const deptNames = record.departments.map(dept => dept.name).join(',');
            // 顺便直接修改原始对象，以便下次渲染时可以直接使用
            record.computedDepartment = deptNames;
            return deptNames;
          }

          // 尝试从departmentName获取
          if (record.departmentName) {
            // 顺便直接修改原始对象，以便下次渲染时可以直接使用
            record.computedDepartment = record.departmentName;
            return record.departmentName;
          }

          // 尝试从多个可能的字段中获取部门信息
          const deptName = record.department ||
                          record.deptName ||
                          (record.deptInfo ? record.deptInfo.name : '') ||
                          (record.dept ? record.dept.name : '') ||
                          (record.departmentList && record.departmentList.length > 0 ? record.departmentList[0].name : '');

          return deptName || '未分配';
        }
      }
    ];

    // 调试辅助
    const debug = (message, data) => {
      console.log(`[CollaboratorSelectionModal] ${message}`, data);
    };

    // 方法
    const loadRoleList = async () => {
      debug('开始加载角色列表', { keyword: roleSearchKeyword.value });
      roleLoading.value = true;
      try {
        const response = await getRoleList({ keyword: roleSearchKeyword.value });
        debug('角色列表响应:', response);

        // 检查响应结构，适配不同的响应格式
        let roleData = [];
        if (response) {
          if (response.data) {
            if (Array.isArray(response.data)) {
              roleData = response.data;
            } else if (response.data.list && Array.isArray(response.data.list)) {
              // 如果是分页格式的响应
              roleData = response.data.list;
            } else if (typeof response.data === 'object') {
              // 尝试其他可能的格式
              const possibleArrays = Object.values(response.data);
              for (const value of possibleArrays) {
                if (Array.isArray(value)) {
                  roleData = value;
                  break;
                } else if (typeof value === 'object' && value !== null) {
                  // 尝试从对象中提取数据
                  roleData = [value];
                  break;
                }
              }

              // 如果上面没找到数组，则把整个data作为对象数组处理
              if (roleData.length === 0) {
                roleData = Object.values(response.data).filter(item => typeof item === 'object' && item !== null);
              }
            }
          } else if (Array.isArray(response)) {
            // 如果整个响应就是一个数组
            roleData = response;
          } else if (typeof response === 'object') {
            // 如果整个响应是对象但没有data字段
            const possibleArrays = Object.values(response);
            for (const value of possibleArrays) {
              if (Array.isArray(value)) {
                roleData = value;
                break;
              }
            }
          }
        }

        // 确保roleList是数组且每个元素有id属性
        roleList.value = roleData.filter(role => role && role.id);

        if (roleList.value.length === 0) {
          debug('角色列表为空或格式不正确，原始响应:', response);
        }
      } catch (error) {
        console.error('获取角色列表失败:', error);
        roleList.value = []; // 出错时确保是空数组
      } finally {
        roleLoading.value = false;
      }
    };

    // 添加变量来存储上一次的搜索关键词
    const lastSearchKeyword = ref('');

    const loadUserList = async () => {
      if (!selectedRoleId.value) {
        debug('未选择角色或角色ID无效，清空用户列表', selectedRoleId.value);
        userList.value = [];
        userPagination.value.total = 0;
        return;
      }

      debug('开始加载角色用户列表', {
        roleId: selectedRoleId.value,
        pageNum: userPagination.value.current,
        pageSize: userPagination.value.pageSize,
        keyword: userSearchKeyword.value
      });

      userLoading.value = true;
      try {
        // 确保使用正确的参数名称调用API
        const params = {
          roleId: selectedRoleId.value,
          id: selectedRoleId.value, // 保持id参数以兼容现有API
          pageNum: userPagination.value.current,
          pageSize: userPagination.value.pageSize
        };

        // 只在有搜索关键词时添加keyword参数
        if (userSearchKeyword.value) {
          params.keyword = userSearchKeyword.value.trim();
        }

        const response = await getRoleUsers(params);
        debug('角色用户列表响应:', response);

        // 检查响应结构，适配不同的响应格式
        let userData = [];
        let total = 0;

        if (response) {
          if (response.data) {
            if (Array.isArray(response.data)) {
              userData = response.data;
              total = userData.length;
            } else if (response.data.list && Array.isArray(response.data.list)) {
              // 如果是分页格式的响应
              userData = response.data.list;
              total = response.data.total || userData.length;
            } else if (typeof response.data === 'object') {
              // 尝试其他可能的格式
              const possibleArrays = Object.values(response.data);
              for (const value of possibleArrays) {
                if (Array.isArray(value)) {
                  userData = value;
                  total = userData.length;
                  break;
                } else if (typeof value === 'object' && value !== null) {
                  // 尝试从对象中提取数据
                  userData = [value];
                  total = 1;
                  break;
                }
              }

              // 如果上面没找到数组，则把整个data作为对象数组处理
              if (userData.length === 0) {
                userData = Object.values(response.data).filter(item => typeof item === 'object' && item !== null);
                total = userData.length;
              }
            }
          } else if (Array.isArray(response)) {
            // 如果整个响应就是一个数组
            userData = response;
            total = userData.length;
          } else if (typeof response === 'object') {
            // 如果整个响应是对象但没有data字段
            const possibleArrays = Object.values(response);
            for (const value of possibleArrays) {
              if (Array.isArray(value)) {
                userData = value;
                total = userData.length;
                break;
              }
            }
          }
        }

        // 确保userList是数组且每个元素有id属性
        let filteredUsers = userData.filter(user => user && (user.id || user.userId));

        // 如果有搜索关键词且数据量没有变化（说明服务端搜索可能没有生效），进行客户端过滤
        if (userSearchKeyword.value &&
            ((filteredUsers.length === userList.value.length && userSearchKeyword.value !== lastSearchKeyword.value) ||
             filteredUsers.length > 10)) {
          const keyword = userSearchKeyword.value.toLowerCase().trim();
          debug('服务端搜索似乎未生效，进行客户端过滤', { keyword, totalBeforeFilter: filteredUsers.length });

          // 本地过滤
          filteredUsers = filteredUsers.filter(user => {
            const username = (user.username || '').toLowerCase();
            const nickname = (user.nickname || '').toLowerCase();
            const employeeId = (user.employeeId || '').toLowerCase();
            const email = (user.email || '').toLowerCase();

            return username.includes(keyword) ||
                  nickname.includes(keyword) ||
                  employeeId.includes(keyword) ||
                  email.includes(keyword);
          });

          debug('客户端过滤后的结果数量:', filteredUsers.length);
          total = filteredUsers.length; // 更新总数为过滤后的数量
        }

        userList.value = filteredUsers;
        lastSearchKeyword.value = userSearchKeyword.value; // 记录最后一次搜索关键词

        userPagination.value.total = total;

        if (userList.value.length === 0) {
          debug('用户列表为空或格式不正确，原始响应:', response);
          return;
        }

        // 立即获取所有用户的部门信息
        await fetchDepartmentsForUsers();

      } catch (error) {
        console.error('获取人员列表失败:', error);
        userList.value = []; // 出错时确保是空数组
        userPagination.value.total = 0;
      } finally {
        userLoading.value = false;
      }
    };

    // 获取用户列表的部门信息
    const fetchDepartmentsForUsers = async () => {
      try {
        const userIds = userList.value.map(user => user.userId || user.id).filter(Boolean);
        if (userIds.length === 0) return;

        debug('获取用户列表的部门信息，用户ID:', userIds);
        const deptResponse = await getUserDepartments(userIds);

        // 详细输出响应结构以便调试
        console.log('部门信息API响应:', deptResponse);

        if (deptResponse && deptResponse.data) {
          let deptData;

          // 确定数据位置
          if (deptResponse.data.data && Array.isArray(deptResponse.data.data)) {
            deptData = deptResponse.data.data;
          } else if (Array.isArray(deptResponse.data)) {
            deptData = deptResponse.data;
          } else {
            console.warn('部门数据结构不符合预期:', deptResponse.data);
            return;
          }

          debug('获取到的部门信息:', deptData);

          // 创建用户ID到部门信息的映射
          const deptMap = new Map();
          deptData.forEach(item => {
            if (item && (item.userId || item.id)) {
              // 使用userId或id作为键
              const key = item.userId || item.id;
              deptMap.set(key, item);
              console.log(`添加部门映射: userId=${key}, 部门=${item.departmentName || '无departmentName'}`);
            }
          });

          // 更新用户列表中的部门信息
          userList.value = userList.value.map(user => {
            const userId = user.userId || user.id;
            console.log(`处理用户列表项: ${user.nickname || user.username}, userId=${userId}`);

            // 尝试用userId和id查找部门
            let deptInfo = deptMap.get(userId);
            if (!deptInfo && user.id !== userId) {
              deptInfo = deptMap.get(user.id);
            }

            if (deptInfo) {
              console.log(`找到用户 ${userId} 的部门数据:`, deptInfo);
              const updatedUser = { ...user };

              // 调试信息
              console.log('用户 ID 信息:', {
                id: user.id,
                userId: user.userId,
                deptInfoId: deptInfo.id,
                deptInfoUserId: deptInfo.userId
              });

              // 复制部门信息
              if (deptInfo.departmentId) {
                updatedUser.departmentId = deptInfo.departmentId;
              }

              if (deptInfo.departmentName) {
                updatedUser.departmentName = deptInfo.departmentName;
                // 如果没有更好的选择，先设置departmentName为computedDepartment
                if (!updatedUser.computedDepartment) {
                  updatedUser.computedDepartment = deptInfo.departmentName;
                }
              }

              // 复制departments数组
              if (deptInfo.departments && Array.isArray(deptInfo.departments) && deptInfo.departments.length > 0) {
                updatedUser.departments = [...deptInfo.departments];

                // 计算部门显示名称
                const deptNames = deptInfo.departments.map(d => d.name).join(',');
                updatedUser.computedDepartment = deptNames;

                console.log(`为用户 ${userId} 设置了departments数组，计算的部门名称: ${deptNames}`);
              } else if (deptInfo.departmentName && !updatedUser.computedDepartment) {
                // 如果设置了departmentName但没有设置computedDepartment
                updatedUser.computedDepartment = deptInfo.departmentName;
                console.log(`为用户 ${userId} 设置了部门名称: ${deptInfo.departmentName}`);
              }

              return updatedUser;
            } else {
              console.log(`未找到用户 ${userId} 的部门数据`);
              return user;
            }
          });

          // 确保每个用户都有computedDepartment
          userList.value.forEach((user, index) => {
            if (!user.computedDepartment) {
              console.log(`用户 ${user.nickname || user.username} 的computedDepartment尚未设置，尝试设置部门`);

              // 尝试计算并设置部门
              const deptName = getUserDepartmentDisplay(user);
              console.log(`为用户 ${user.nickname || user.username} 计算的部门名称: ${deptName}`);

              // 验证部门名称是否被设置
              console.log(`设置后用户 ${user.nickname || user.username} 的部门信息:`, {
                computedDepartment: user.computedDepartment,
                departmentName: user.departmentName,
                departments: user.departments
              });
            }
          });
        }
      } catch (error) {
        console.error('获取用户部门信息失败:', error);
      }
    };

    const handleRoleSearch = () => {
      loadRoleList();
    };

    const handleUserSearch = () => {
      // 重置分页到第一页
      userPagination.value.current = 1;
      // 确保关键词被传递到后端API
      debug('执行用户搜索', { keyword: userSearchKeyword.value });
      loadUserList();
    };

    // 角色下拉框选择变更
    const handleRoleChange = (value) => {
      debug('角色选择变更', { roleId: value });
      selectedRoleId.value = value;
      loadUserList();
    };

    // 检查用户是否已经被选中
    const isUserSelected = (userId) => {
      if (!userId) return false;
      return selectedUserIds.value.includes(userId);
    };

    const onUserSelect = (selectedRowKeys, selectedRows) => {
      debug('用户选择变更', { selectedRowKeys, selectedRows });

      // 只处理当前显示用户的选择变更，不重置已有选择
      // 1. 查找当前新选中的用户（过滤掉已选中的用户）
      const newSelectedUsers = selectedRows.filter(row => !isUserSelected(row.id || row.userId));

      // 2. 查找当前被取消选中的用户ID（从当前页用户中）
      const currentUserIds = userList.value.map(user => user.id || user.userId).filter(Boolean);
      const deselectedIds = selectedUserIds.value.filter(id =>
        currentUserIds.includes(id) && !selectedRowKeys.includes(id)
      );

      // 3. 更新已选用户列表(添加新选中的)
      if (newSelectedUsers.length > 0) {
        debug('添加新选中用户', newSelectedUsers);
        selectedUsers.value = [...selectedUsers.value, ...newSelectedUsers];
        // 不再需要单独获取部门信息，因为已经在loadUserList中处理过了
      }

      // 4. 移除被取消的用户
      if (deselectedIds.length > 0) {
        debug('移除取消选择的用户', deselectedIds);
        selectedUsers.value = selectedUsers.value.filter(user => {
          const userId = user.id || user.userId;
          return !deselectedIds.includes(userId);
        });
      }

      // 5. 更新已选用户ID集合
      selectedUserIds.value = selectedUsers.value.map(user => user.id || user.userId).filter(Boolean);

      debug('更新后的选择状态', {
        selectedUserIds: selectedUserIds.value,
        selectedUsers: selectedUsers.value.length
      });
    };

    const removeSelectedUser = (user) => {
      if (!user) return;

      const userId = user.id || user.userId;
      if (!userId) return;

      selectedUsers.value = selectedUsers.value.filter(u => {
        const id = u.id || u.userId;
        return id !== userId;
      });

      selectedUserIds.value = selectedUserIds.value.filter(id => id !== userId);
    };

    const clearSelectedUsers = () => {
      selectedUsers.value = [];
      selectedUserIds.value = [];
    };

    const handleCancel = () => {
      console.log('handleCancel');
      emit('update:visible', false);
      emit('cancel');
      clearSelectedUsers();
    };

    const handleConfirm = () => {
      try {
        // 确保返回一个标准格式的用户数组
        const normalizedUsers = Array.isArray(selectedUsers.value) ?
          selectedUsers.value
            .filter(user => user && typeof user === 'object' && user.id)
            .map(user => {
              return {
                id: user.id,
                username: user.username || '',
                nickname: user.nickname || user.username || '',
                // 添加部门信息
                department: getUserDepartmentDisplay(user),
                departmentId: user.departmentId ||
                           (user.deptInfo ? user.deptInfo.id : '') ||
                           (user.dept ? user.dept.id : '') ||
                           (user.departmentList && user.departmentList.length > 0 ? user.departmentList[0].id : '') ||
                           ''
              };
            }) : [];

        // 发送确认事件
        emit('confirm', normalizedUsers);
      } catch (error) {
        console.error('处理协同人员数据出错:', error);
        emit('confirm', []);
      } finally {
        handleCancel();
      }
    };

    // 获取当前角色下的已选择行键值
    const getSelectedRowKeys = () => {
      const currentUserIds = userList.value.map(user => user.id || user.userId).filter(Boolean);
      return selectedUserIds.value.filter(id => currentUserIds.includes(id));
    };

    // 处理分页变化
    const handleTableChange = (pagination) => {
      debug('分页变化', pagination);
      userPagination.value.current = pagination.current;
      userPagination.value.pageSize = pagination.pageSize;
      loadUserList();
    };

    // 初始化和重置
    const initialize = () => {
      roleList.value = [];
      userList.value = [];
      selectedRoleId.value = null;
      // 不重置选择项
      // selectedUserIds.value = [];
      // selectedUsers.value = [];
    };

    // 监听
    watch(() => props.visible, (newVal) => {
      if (newVal) {
        debug('弹窗显示，初始化数据', { visible: newVal });
        initialize();
        loadRoleList();
      }
    });

    // 修改watch hook，因为现在部门信息在列表加载时就获取了
    watch(() => selectedUsers.value.length, (newVal, oldVal) => {
      // 不再需要检查和获取部门信息
      debug(`已选用户数量变化: ${oldVal} -> ${newVal}`);
    });

    // 组件挂载时添加调试信息
    onMounted(() => {
      debug('组件已挂载', { visible: props.visible });
      // 如果有已选择的用户，确保他们有正确的部门信息
      if (selectedUsers.value.length > 0) {
        // 获取用户的部门信息
        const userIds = selectedUsers.value.map(user => user.userId || user.id).filter(Boolean);
        if (userIds.length > 0) {
          getUserDepartments(userIds).then(response => {
            if (response && response.data && response.data.data) {
              const deptData = response.data.data;

              // 创建用户ID到部门信息的映射
              const deptMap = new Map();
              deptData.forEach(item => {
                if (item && item.userId) {
                  deptMap.set(item.userId, item);
                }
              });

              // 更新已选用户的部门信息
              selectedUsers.value = selectedUsers.value.map(user => {
                const userId = user.userId || user.id;
                const deptInfo = deptMap.get(userId);

                if (deptInfo) {
                  const updatedUser = { ...user };

                  // 复制部门信息
                  if (deptInfo.departmentId) {
                    updatedUser.departmentId = deptInfo.departmentId;
                  }

                  if (deptInfo.departmentName) {
                    updatedUser.departmentName = deptInfo.departmentName;
                  }

                  // 复制departments数组
                  if (deptInfo.departments && Array.isArray(deptInfo.departments) && deptInfo.departments.length > 0) {
                    updatedUser.departments = [...deptInfo.departments];
                    updatedUser.computedDepartment = deptInfo.departments.map(d => d.name).join(',');
                  } else if (deptInfo.departmentName) {
                    updatedUser.computedDepartment = deptInfo.departmentName;
                  }

                  return updatedUser;
                }

                return user;
              });
            }
          }).catch(error => {
            console.error('获取已选用户部门信息失败:', error);
          });
        }
      }
    });

    // 获取用户部门显示文本
    const getUserDepartmentDisplay = (user) => {
      if (!user) {
        console.warn('getUserDepartmentDisplay: 传入的用户对象为空');
        return '未分配';
      }

      // 调试信息
      console.log(`获取用户[${user.nickname || user.username}]的部门信息:`, {
        id: user.id,
        computedDepartment: user.computedDepartment,
        departments: user.departments,
        departmentName: user.departmentName,
        department: user.department,
        deptName: user.deptName
      });

      // 首先检查是否有计算好的部门名称
      if (user.computedDepartment) {
        console.log(`用户[${user.nickname || user.username}]使用computedDepartment: ${user.computedDepartment}`);
        return user.computedDepartment;
      }

      // 尝试从departments数组中获取
      if (user.departments && Array.isArray(user.departments) && user.departments.length > 0) {
        console.log(`用户[${user.nickname || user.username}]从departments数组获取部门`);
        const deptNames = user.departments.map(dept => dept.name).join(',');
        // 更新用户对象的computedDepartment属性
        user.computedDepartment = deptNames;
        return deptNames;
      }

      // 尝试从departmentName获取
      if (user.departmentName) {
        console.log(`用户[${user.nickname || user.username}]使用departmentName: ${user.departmentName}`);
        // 更新用户对象的computedDepartment属性
        user.computedDepartment = user.departmentName;
        return user.departmentName;
      }

      // 尝试从多个可能的字段中获取部门信息
      const deptName = user.department ||
                     user.deptName ||
                     (user.deptInfo ? user.deptInfo.name : '') ||
                     (user.dept ? user.dept.name : '') ||
                     (user.departmentList && user.departmentList.length > 0 ? user.departmentList[0].name : '');

      if (deptName) {
        console.log(`用户[${user.nickname || user.username}]从其他字段获取部门: ${deptName}`);
        // 更新用户对象的computedDepartment属性
        user.computedDepartment = deptName;
        return deptName;
      }

      console.warn(`用户[${user.nickname || user.username}]未找到部门信息`);
      return '未分配';
    };

    // 获取表格空状态文本
    const getEmptyText = () => {
      if (!selectedRoleId.value) {
        return '请先选择角色';
      }

      if (userSearchKeyword.value) {
        return `未找到匹配 "${userSearchKeyword.value}" 的人员`;
      }

      return '该角色下暂无人员';
    };

    return {
      userSearchKeyword,
      roleList,
      userList,
      selectedRoleId,
      roleLoading,
      userLoading,
      selectedUserIds,
      selectedUsers,
      roleOptions,
      userColumns,
      userPagination,
      getSelectedRowKeys,
      isUserSelected,
      handleRoleSearch,
      handleUserSearch,
      handleRoleChange,
      handleTableChange,
      onUserSelect,
      removeSelectedUser,
      clearSelectedUsers,
      handleCancel,
      handleConfirm,
      getUserDepartmentDisplay,
      getEmptyText
    };
  }
});
</script>

<style lang="less" scoped>
.collaborator-selection {
  display: flex;
  gap: 16px;
  min-height: 500px;

  .user-list-container,
  .selected-users-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden; // 防止溢出

    .ant-card {
      height: 100%;
      display: flex;
      flex-direction: column;

      .ant-card-body {
        flex: 1;
        overflow: auto; // 内容过多时滚动
        display: flex;
        flex-direction: column;
      }
    }
  }

  .user-list-container {
    flex: 2; // 让用户列表区域更宽
  }

  .filter-area {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    flex-wrap: wrap; // 小屏幕自动换行
    gap: 8px;

    .filter-item {
      display: flex;
      align-items: center;

      .label {
        margin-right: 8px;
        white-space: nowrap;
        font-size: 14px;
      }
    }
  }

  .ant-table-body {
    overflow-y: auto;
  }

  .ant-table-wrapper {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .ant-table {
    flex: 1;
    overflow: auto;
  }

  .ant-table-row {
    cursor: pointer;
    &:hover {
      background-color: #f5f5f5;
    }
  }

  .selected-users-list {
    overflow-y: auto;
    max-height: 400px;
  }

  .selected-users-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .selected-user-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;

    .user-info {
      display: flex;
      align-items: center;

      .user-name {
        font-weight: 500;
        margin-left: 8px;
      }

      .user-dept {
        font-size: 12px;
        color: rgba(0, 0, 0, 0.45);
        margin-left: 8px;
        max-width: 200px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

.modal-footer {
  margin-top: 24px;
  text-align: right;
}
</style>