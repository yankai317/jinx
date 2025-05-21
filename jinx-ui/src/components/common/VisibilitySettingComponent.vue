<template>
  <div class="visibility-setting-container">
    <div class="selection-header">
      <div class="selection-title">选择可见范围</div>
      <div class="selection-find-link">
        <a href="javascript:void(0)">找不到我要选的人或部门?</a>
      </div>
    </div>

    <UserAndGroupSelect
    :preSelectedItems="selectedRange" @change="infoChange" />

    <div class="action-buttons">
      <a-button type="primary" @click="handleConfirm">确定</a-button>
      <a-button @click="handleCancel">取消</a-button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, defineComponent, watch } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, SearchOutlined, CloseOutlined } from '@ant-design/icons-vue';
import { updateRange, queryOrgTree, queryRoles, queryUsers, queryOrgUsers } from '@/api/common';
import { addUsersToRole } from '@/api/role';
import UserAndGroupSelect from './UserAndGroupSelect.vue';

export default defineComponent({
  name: 'VisibilitySettingComponent',
  components: {
    PlusOutlined,
    DeleteOutlined,
    SearchOutlined,
    CloseOutlined,
    UserAndGroupSelect
  },
  props: {
    bizId: {
      type: Number,
      required: true
    },
    bizType: {
      type: String,
      default: 'course'
    },
    title: {
      type: String,
      default: '可见范围设置'
    },
    selectedRange: {
      type: Array,
      default: () => []
    }
  },
  emits: ['success', 'cancel'],
  setup(props, { emit }) {
    const loading = ref(false);
    const activeKey = ref('org');

    // Navigation path
    const currentPath = ref([{ name: '根目录', key: 'root' }]);
    const currentItems = ref([]);

    // Store all items for search
    const allItems = ref({
      org: [],
      role: [],
      position: [],
      user: []
    });

    // Store enriched department tree (with users)
    const enrichedOrgTree = ref([]);

    // Cache for department users to avoid redundant API calls
    const departmentUsersCache = ref({});

    // Selected items
    const selectedTargets = ref([]);

    // Current navigation stack
    const navigationStack = ref([]);

    // Check if an item has children
    const hasChildren = (item) => {
      // 部门可能有子部门或用户
      if (item.type === 'department') {
        return (item.children && item.children.length > 0) || item.hasUsers === true;
      }
      // 其他类型只检查children
      return item.children && item.children.length > 0;
    };

    /** 范围修改 */
    const infoChange = (value) => {
      console.log('infoChange', value)
      selectedTargets.value = value
    }



    // Load initial items based on active key
    const loadInitialItems = async () => {
      loading.value = true;
      try {
        let items = [];
        switch (activeKey.value) {
          case 'org':
            if (enrichedOrgTree.value.length === 0) {
              // 第一次加载时，创建包含所有用户的完整树结构
              // await createFullOrgTreeWithUsers();
            }
            items = enrichedOrgTree.value;

            // 加载根级的用户（通常是没有部门的用户）
            try {
              if (!departmentUsersCache.value['root']) {
                const rootUsers = await loadDepartmentUsers(0); // 使用0作为根级部门ID
                departmentUsersCache.value['root'] = rootUsers;
                if (rootUsers && rootUsers.length > 0) {
                  // 将根级用户添加到显示列表
                  items = [...items, ...rootUsers];
                }
              } else {
                items = [...items, ...departmentUsersCache.value['root']];
              }
            } catch (error) {
              console.error('加载根级用户失败:', error);
            }

            break;
          case 'role':
            items = await loadRoles();
            break;
          case 'position':
            items = await loadPositions();
            break;
          case 'user':
            items = await loadUsers();
            break;
        }

        allItems.value[activeKey.value] = items;
        currentItems.value = items;

        // Reset navigation
        currentPath.value = [{ name: '根目录', key: 'root' }];
        navigationStack.value = [];

      } catch (error) {
        console.error('加载数据失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 创建完整的组织结构树（包括所有部门和用户）
    const createFullOrgTreeWithUsers = async () => {
      try {
        // 首先加载组织树结构
        const departments = await loadDepartments();

        // 递归加载每个部门下的用户
        const loadDepartmentUsersRecursively = async (deptList) => {
          if (!deptList || deptList.length === 0) return;

          for (const dept of deptList) {
            // 加载当前部门的用户
            if (!departmentUsersCache.value[dept.id]) {
              const users = await loadDepartmentUsers(dept.id);
              departmentUsersCache.value[dept.id] = users;

              // 把用户作为叶子节点添加到部门中
              if (users && users.length > 0) {
                if (!dept.allChildren) {
                  dept.allChildren = [...(dept.children || [])];
                }
                dept.allChildren = [...dept.allChildren, ...users];

                // 标记该部门拥有用户，用于显示下级按钮
                dept.hasUsers = true;
              }
            }

            // 递归处理子部门
            if (dept.children && dept.children.length > 0) {
              await loadDepartmentUsersRecursively(dept.children);
            }
          }
        };

        // 开始递归加载
        await loadDepartmentUsersRecursively(departments);

        // 存储完整的树
        enrichedOrgTree.value = departments;
      } catch (error) {
        console.error('创建完整组织树失败:', error);
      }
    };

    // Watch for active key changes
    watch(activeKey, () => {
      loadInitialItems();
    });

    // Format departments data
    const formatDepartmentData = (departments) => {
      return departments.map(dept => ({
        key: `dept-${dept.id}`,
        id: dept.id,
        title: dept.name,
        type: 'department',
        children: dept.children ? formatDepartmentData(dept.children) : []
      }));
    };

    // Load departments
    const loadDepartments = async () => {
      const res = await queryOrgTree();
      return res.code === 200 ? formatDepartmentData(res.data || []) : [];
    };

    // Load department users
    const loadDepartmentUsers = async (departmentId) => {
      if (!departmentId) {
        console.error('调用queryOrgUsers时部门ID不能为空');
        return [];
      }

      try {
        const res = await queryOrgUsers({ id: departmentId });

        if (res.code !== 200) {
          return [];
        }

        // Format users for display - use userId instead of id
        return (res.data || []).map(user => ({
          key: `user-${user.userId || user.id}`,
          id: user.userId || user.id, // Prefer userId, fall back to id if not present
          title: user.nickname || user.username,
          type: 'user'
        }));
      } catch (error) {
        console.error('加载部门用户失败:', error);
        return [];
      }
    };

    // Load roles
    const loadRoles = async () => {
      const res = await queryRoles();
      return res.code === 200 ? (res.data || []).map(role => ({
        key: `role-${role.id}`,
        id: role.id,
        title: role.name,
        type: 'role'
      })) : [];
    };

    // Load positions (implement according to your API)
    const loadPositions = async () => {
      // Implement position loading logic
      return [];
    };

    // Load users
    const loadUsers = async () => {
      const res = await queryUsers();
      return res.code === 200 ? (res.data || []).map(user => ({
        key: `user-${user.userId || user.id}`,
        id: user.userId || user.id, // Prefer userId, fall back to id if not present
        title: user.nickname || user.username,
        type: 'user'
      })) : [];
    };

    // Handle confirm
    const handleConfirm = async () => {
      if (selectedTargets.value.length === 0) {
        message.warning('请至少选择一个可见范围');
        return;
      }

      loading.value = true;
      try {
        // 如果是角色管理，需要使用专门的API添加用户到角色
        if (props.bizType === 'role') {
          // 提取所有用户ID
          const userIds = [];
          selectedTargets.value.forEach(target => {
            if (target.type === 'user') {
              userIds.push(target.id);
            }
          });

          if (userIds.length === 0) {
            message.warning('请至少选择一个用户');
            loading.value = false;
            return;
          }

          // 调用添加用户到角色的API
          const res = await addUsersToRole({
            roleId: props.bizId,
            userIds: userIds
          });

          console.log('添加用户到角色响应:', res);

          if (res.code === 200) {
            message.success(`添加成功: ${res.data?.success || 0}个用户, 失败: ${res.data?.fail || 0}个用户`);
            // 即使有部分失败也触发成功事件，让父组件刷新数据
            emit('success');
          } else {
            message.error(res.message || '添加用户到角色失败');
          }
        } else {
          // 其他业务类型使用通用的updateRange API
          const requestData = {
            modelType: 'visibility',
            type: props.bizType,
            typeId: props.bizId,
            targetTypeAndIds: {}
          };

          // Group selected targets by type
          selectedTargets.value.forEach(target => {
            if (!requestData.targetTypeAndIds[target.type]) {
              requestData.targetTypeAndIds[target.type] = [];
            }
            requestData.targetTypeAndIds[target.type].push(target.id);
          });

          const res = await updateRange(requestData);

          if (res.code === 200) {
            message.success('可见范围设置成功');
            emit('success');
          } else {
            message.error(res.message || '可见范围设置失败');
          }
        }
      } catch (error) {
        console.error('操作失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // Handle cancel
    const handleCancel = () => {
      emit('cancel');
    };

    onMounted(() => {
      loadInitialItems();
    });

    return {
      loading,
      activeKey,
      selectedTargets,
      hasChildren,
      infoChange,
      handleConfirm,
      handleCancel
    };
  }
});
</script>

<style scoped>
.visibility-setting-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 240px);
  background-color: #fff;
  padding: 16px;
}

.selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selection-title {
  font-size: 16px;
  font-weight: 500;
}

.selection-find-link a {
  color: #1890ff;
  font-size: 14px;
}

.action-buttons {
  margin-top: 24px;
  text-align: right;
}

.action-buttons .ant-btn+.ant-btn {
  margin-left: 8px;
}
</style>