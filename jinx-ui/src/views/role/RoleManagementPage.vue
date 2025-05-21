<template>
  <a-layout class="role-management">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent
        v-model:collapsed="collapsed"
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal"
      />

      <a-layout-content class="content">
        <div class="content-header">
          <PageBreadcrumb />
        </div>
        <div class="role-management-container">
          <!-- 左侧角色列表 -->
          <div class="role-list-container">
            <a-card title="角色列表" class="role-card">
              <!-- 搜索和操作区域 -->
              <div class="operation-area">
                <a-row :gutter="16">
                  <a-col :span="14">
                    <a-input-search
                      v-model:value="searchKeyword"
                      placeholder="搜索角色名称"
                      @search="handleSearch"
                      allow-clear
                    />
                  </a-col>
                  <a-col :span="10" style="text-align: right">
                    <a-button type="primary" @click="handleCreateRole">
                      <template #icon><plus-outlined /></template>
                      创建角色
                    </a-button>
                  </a-col>
                </a-row>
              </div>

              <!-- 角色列表 -->
              <a-table
                :columns="roleColumns"
                :data-source="roleList"
                :loading="roleLoading"
                :pagination="rolePagination"
                @change="handleRoleTableChange"
                row-key="id"
                :rowClassName="(record) => record.id === currentRole?.id ? 'selected-row' : ''"
                @row-click="handleRoleRowClick"
                :customRow="customRowHandler"
              >
              </a-table>
            </a-card>
          </div>

          <!-- 右侧人员列表 -->
          <div class="user-list-container">
            <a-card class="user-card">
              <!-- 标题和操作区域 -->
              <template #title>
                <div class="user-card-header">
                  <span class="user-card-title">{{ currentRole?.roleName || '角色' }} - 人员列表</span>
                  <div class="user-card-actions">
                    <a-space>
                      <a-button
                        type="primary"
                        @click="handleAddUsers"
                        :disabled="!currentRole"
                        class="action-button"
                      ><template #icon><user-add-outlined /></template>
                        添加人员
                      </a-button>
                      <a-button
                        type="primary"
                        @click="handleEditRole(currentRole)"
                        :disabled="!currentRole"
                        class="action-button"
                      >
                        <template #icon><edit-outlined /></template>
                        修改角色权限
                      </a-button>
                      <a-popconfirm
                        title="确定要删除该角色吗？"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="handleDeleteRole(currentRole)"
                        :disabled="!currentRole"
                      >
                        <a-button
                          danger
                          :disabled="!currentRole"
                          class="action-button"
                        >
                          <template #icon><delete-outlined /></template>
                          删除角色
                        </a-button>
                      </a-popconfirm>
                    </a-space>
                  </div>
                </div>
              </template>

              <!-- 人员列表 -->
              <div class="batch-operation" v-if="currentRole">
                <a-button
                  type="primary"
                  danger
                  :disabled="selectedUserIds.length === 0"
                  @click="handleBatchRemoveUsers"
                >
                  <template #icon><delete-outlined /></template>
                  批量删除
                </a-button>
              </div>
              <a-table
                :columns="userColumns"
                :data-source="userList"
                :loading="userLoading"
                :pagination="userPagination"
                @change="handleUserTableChange"
                row-key="id"
                :locale="{emptyText: currentRole ? '该角色下暂无人员' : '请先选择左侧角色'}"
                :row-selection="currentRole ? { selectedRowKeys: selectedUserIds, onChange: onSelectChange } : null"
              >
              </a-table>
            </a-card>
          </div>
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加人员弹窗 --><a-modal
      v-model:visible="addUsersModalVisible"
      title="添加人员"
      width="800px"
      :footer="null"
      @cancel="handleAddUsersCancel"
    >
      <visibility-setting-component
        v-if="addUsersModalVisible"
        :biz-id="currentRole?.id"
        biz-type="role"
        title="添加人员"
        @success="handleAddUsersSuccess"
        @cancel="handleAddUsersCancel"
      />
    </a-modal></a-layout>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, watch } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined, UserAddOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import { useRouter } from 'vue-router';
import { getRoleList, deleteRole, getRoleUsers, removeUsersFromRole } from '@/api/role';
import VisibilitySettingComponent from '@/components/common/VisibilitySettingComponent.vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';

export default defineComponent({
  name: 'RoleManagementPage',
  components: {
    PlusOutlined,
    UserAddOutlined,
    EditOutlined,
    DeleteOutlined,
    VisibilitySettingComponent,
    HeaderComponent,
    SiderComponent,
    PageBreadcrumb
  },
  setup() {
    const router = useRouter();
    const roleLoading = ref(false);
    const userLoading = ref(false);
    const searchKeyword = ref('');
    const roleList = ref([]);
    const userList = ref([]);
    const currentRole = ref(null);
    const addUsersModalVisible = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['role']);
    const openKeys = ref(['system']);
    const selectedUserIds = ref([]);

    // 角色表格列定义
    const roleColumns = [
      {
        title: '角色名称',
        dataIndex: 'roleName',
        key: 'roleName'
      },
      {
        title: '角色编码',
        dataIndex: 'roleCode',
        key: 'roleCode'
      },
      {
        title: '角色描述',
        dataIndex: 'roleDescription',
        key: 'roleDescription',
        ellipsis: true
      }
    ];

    // 用户表格列定义
    const userColumns = [
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username'
      },
      {
        title: '昵称',
        dataIndex: 'nickname',
        key: 'nickname'
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email'
      },
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone'
      }
    ];

    // 角色分页配置
    const rolePagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 用户分页配置
    const userPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 获取角色列表
    const fetchRoleList = async () => {
      roleLoading.value = true;
      try {
        const res = await getRoleList({
          keyword: searchKeyword.value
        });

        if (res.code === 200 && res.data) {
          roleList.value = res.data.data || [];
          rolePagination.total = roleList.value.length;

          // 如果当前选中的角色不在列表中，清空选择
          if (currentRole.value && !roleList.value.find(role => role.id === currentRole.value.id)) {
            currentRole.value = null;
            userList.value = [];
          }
        } else {
          message.error(res.message || '获取角色列表失败');
        }
      } catch (error) {
        console.error('获取角色列表失败:', error);
      } finally {
        roleLoading.value = false;
      }
    };

    // 获取角色下的用户列表
    const fetchRoleUsers = async (roleId) => {
      if (!roleId) return;

      userLoading.value = true;
      selectedUserIds.value = []; // Clear selected users when refreshing
      try {
        const res = await getRoleUsers({ id: roleId });
        console.log('获取角色用户列表返回数据:', res);

        if (res.code === 200) {
          // 检查返回数据的结构，确保正确处理
          if (Array.isArray(res.data)) {
            userList.value = res.data;
          } else if (res.data && Array.isArray(res.data.data)) {
            userList.value = res.data.data;
          } else if (res.data) {
            userList.value = [].concat(res.data);
          } else {
            userList.value = [];
          }
          userPagination.total = userList.value.length;
          userPagination.current = 1; // Reset to first page after refresh
        } else {
          message.error(res.message || '获取角色用户列表失败');
          // 确保即使API返回错误也清空用户列表，避免显示旧数据
          userList.value = [];
        }
      } catch (error) {
        console.error('获取角色用户列表失败:', error);
        // 确保即使API调用失败也清空用户列表，避免显示旧数据
        userList.value = [];
      } finally {
        userLoading.value = false;
      }
    };

    // 处理搜索
    const handleSearch = () => {
      rolePagination.current = 1;
      fetchRoleList();
    };

    // 处理角色表格变化
    const handleRoleTableChange = (pag) => {
      rolePagination.current = pag.current;
      rolePagination.pageSize = pag.pageSize;
    };

    // 处理用户表格变化
    const handleUserTableChange = (pag) => {
      userPagination.current = pag.current;
      userPagination.pageSize = pag.pageSize;
    };

    // 处理角色行点击
    const handleRoleRowClick = (record) => {
      // 无论是否点击当前已选中的角色，都设置currentRole并刷新用户列表
      //创建一个新的对象引用，确保Vue能够检测到变化
      currentRole.value = {...record};
      userPagination.current = 1;
      fetchRoleUsers(record.id);
    };

    // 自定义行属性处理函数
    const customRowHandler = (record) => {
      return {
        onClick: () => {
          handleRoleRowClick(record);
        },
        // 确保选中状态正确应用
        class: record.id === currentRole.value?.id ? 'selected-row' : ''
      };
    };

    // 处理创建角色
    const handleCreateRole = () => {
      router.push('/role/create');
    };

    // 处理编辑角色
    const handleEditRole = (record) => {
      if (!record) return;
      router.push(`/role/edit/${record.id}`);
    };

    // 处理删除角色
    const handleDeleteRole = async (record) => {
      if (!record) return;

      roleLoading.value = true;
      try {
        const res = await deleteRole({ id: record.id });

        if (res.code === 200 && res.data) {
          message.success('删除成功');
          fetchRoleList();

          // 如果删除的是当前选中的角色，清空选择
          if (currentRole.value && currentRole.value.id === record.id) {
            currentRole.value = null;
            userList.value = [];
          }
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除角色失败:', error);
      } finally {
        roleLoading.value = false;
      }
    };

    // 处理添加人员
    const handleAddUsers = () => {
      if (!currentRole.value) return;
      addUsersModalVisible.value = true;
    };

    // 处理添加人员取消
    const handleAddUsersCancel = () => {
      addUsersModalVisible.value = false;
    };

    // 处理添加人员成功
    const handleAddUsersSuccess = () => {
      message.success('添加人员成功');
      addUsersModalVisible.value = false;

      // 刷新用户列表
      if (currentRole.value) {
        // 使用setTimeout来确保在DOM更新后再刷新数据
        setTimeout(() => {
          fetchRoleUsers(currentRole.value.id);
        }, 100);
      }
    };

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      // 这里可以添加类目管理弹窗的逻辑，如果需要的话
    };

    onMounted(() => {
      fetchRoleList();
    });

    // 处理表格行选择变化
    const onSelectChange = (selectedRowKeys) => {
      selectedUserIds.value = selectedRowKeys;
    };

    // 处理批量删除用户
    const handleBatchRemoveUsers = async () => {
      if (selectedUserIds.value.length === 0) {
        message.warning('请先选择要删除的人员');
        return;
      }

      try {
        userLoading.value = true;
        const res = await removeUsersFromRole({
          roleId: currentRole.value.id,
          userIds: selectedUserIds.value
        });

        if (res.code === 200) {
          message.success('批量删除成功');
          // 刷新用户列表
          fetchRoleUsers(currentRole.value.id);
          // 清空选择
          selectedUserIds.value = [];
        } else {
          message.error(res.message || '批量删除失败');
        }
      } catch (error) {
        console.error('批量删除人员失败:', error);
      } finally {
        userLoading.value = false;
      }
    };

    return {
      roleLoading,
      userLoading,
      searchKeyword,
      roleList,
      userList,
      roleColumns,
      userColumns,
      rolePagination,
      userPagination,
      currentRole,
      addUsersModalVisible,
      collapsed,
      selectedKeys,
      openKeys,
      selectedUserIds,
      onSelectChange,
      handleSearch,
      handleRoleTableChange,
      handleUserTableChange,
      handleRoleRowClick,
      handleCreateRole,
      handleEditRole,
      handleDeleteRole,
      handleAddUsers,
      handleAddUsersCancel,
      handleAddUsersSuccess,
      handleBatchRemoveUsers,
      showCategoryModal,
      customRowHandler
    };
  }
});
</script>

<style scoped>
.role-management {
  height: 100%;
  background: #f0f2f5;
}

.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  z-index: 1;
}

.content {
  padding: 24px;
  overflow-y: auto;
}

.content-header{
  padding-bottom: 12px;
}

.role-management-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 180px);
}

.role-list-container {
  width: 40%;
  display: flex;
  flex-direction: column;
}

.user-list-container {
  width: 60%;
  display: flex;
  flex-direction: column;
}

.role-card, .user-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.role-card :deep(.ant-card-body),
.user-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.user-card-title {
  font-size: 16px;
  font-weight: 500;
}

.user-card-actions {
  display: flex;
  justify-content: flex-end;
}

.operation-area {
  margin-bottom: 16px;
}

.selected-row {
  background-color: #e6f7ff !important;
  font-weight: 500;
}

/* 鼠标悬停在角色行上时的样式 */
:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #e6f7ff;
  cursor: pointer;
}

:deep(.ant-table-wrapper) {
  flex: 1;
  overflow: auto;
}

.action-button {
  transition: all 0.3s;
}

.action-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.batch-operation {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
