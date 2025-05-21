<template>
  <a-layout class="role-create">
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
        <a-page-header
          title="创建角色"
          sub-title="创建新的系统角色"
          @back="goBack"
        />
      <a-card>
        <a-form
          :model="formState"
          :rules="rules"
          ref="formRef"
          layout="vertical"
        >
          <a-form-item label="角色名称" name="roleName">
            <a-input v-model:value="formState.roleName" placeholder="请输入角色名称" />
          </a-form-item>

          <a-form-item label="角色编码" name="roleCode">
            <a-input v-model:value="formState.roleCode" placeholder="请输入角色编码" />
          </a-form-item>

          <a-form-item label="角色描述" name="roleDescription">
            <a-textarea
              v-model:value="formState.roleDescription"
              placeholder="请输入角色描述"
              :rows="4"
            />
          </a-form-item>

          <a-form-item label="权限设置" name="permissionIds">
            <a-spin :spinning="permissionsLoading">
              <a-tree
                v-if="permissionTree.length > 0"
                checkable
                :tree-data="permissionTree"
                v-model:checked-keys="formState.permissionIds"
                :default-expanded-keys="expandedKeys"
              />
              <a-empty v-else description="暂无权限数据" />
            </a-spin>
          </a-form-item>

          <a-form-item>
            <a-button type="primary" @click="handleSubmit" :loading="submitting">提交</a-button>
            <a-button style="margin-left: 10px" @click="goBack">取消</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { createRole } from '@/api/role';
import { getAllPermissions } from '@/api/permission';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';

export default defineComponent({
  name: 'RoleCreatePage',
  components: {
    HeaderComponent,
    SiderComponent
  },
  setup() {
    const router = useRouter();
    const formRef = ref(null);
    const submitting = ref(false);
    const permissionsLoading = ref(false);
    const permissionTree = ref([]);
    const expandedKeys = ref([]);
    const collapsed = ref(false);
    const selectedKeys = ref(['role']);
    const openKeys = ref(['system']);

    // 表单数据
    const formState = reactive({
      roleName: '',
      roleCode: '',
      roleDescription: '',
      permissionIds: []
    });

    // 表单验证规则
    const rules = {
      roleName: [
        { required: true, message: '请输入角色名称', trigger: 'blur' },
        { max: 50, message: '角色名称不能超过50个字符', trigger: 'blur' }
      ],
      roleCode: [
        { required: true, message: '请输入角色编码', trigger: 'blur' },
        { max: 50, message: '角色编码不能超过50个字符', trigger: 'blur' },
        { pattern: /^[A-Z0-9_]+$/, message: '角色编码只能包含大写字母、数字和下划线', trigger: 'blur' }
      ],
      roleDescription: [
        { max: 255, message: '角色描述不能超过255个字符', trigger: 'blur' }
      ],
      permissionIds: [
        { type: 'array', required: true, message: '请至少选择一个权限', trigger: 'change' }
      ]
    };

    // 获取所有权限
    const fetchPermissions = async () => {
      permissionsLoading.value = true;
      try {
        const res = await getAllPermissions();

        if (res.code === 200 && res.data) {
          permissionTree.value = formatPermissionTree(res.data);

          // 设置默认展开的节点
          const keys = [];
          collectKeys(permissionTree.value, keys);
          expandedKeys.value = keys;
        } else {
          message.error(res.message || '获取权限列表失败');
        }
      } catch (error) {
        console.error('获取权限列表失败:', error);
      } finally {
        permissionsLoading.value = false;
      }
    };

    // 格式化权限树
    const formatPermissionTree = (permissions) => {
      return permissions.map(permission => ({
        title: permission.name,
        key: permission.id,
        children: permission.children && permission.children.length > 0
          ? formatPermissionTree(permission.children)
          : undefined
      }));
    };

    // 收集所有节点的key，用于默认展开
    const collectKeys = (tree, keys) => {
      tree.forEach(node => {
        keys.push(node.key);
        if (node.children && node.children.length > 0) {
          collectKeys(node.children, keys);
        }
      });
    };

    // 提交表单
    const handleSubmit = () => {
      formRef.value.validate().then(async () => {
        submitting.value = true;
        try {
          const res = await createRole(formState);

          if (res.code === 200) {
            message.success('创建角色成功');
            router.push('/role/management');
          } else {
            message.error(res.message || '创建角色失败');
          }
        } catch (error) {
          console.error('创建角色失败:', error);
        } finally {
          submitting.value = false;
        }
      }).catch(error => {
        console.log('验证失败:', error);
      });
    };

    // 返回上一页
    const goBack = () => {
      router.push('/role/management');
    };

    onMounted(() => {
      fetchPermissions();
    });

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      // 这里可以添加类目管理弹窗的逻辑，如果需要的话
    };

    return {
      formRef,
      formState,
      rules,
      submitting,
      permissionsLoading,
      permissionTree,
      expandedKeys,
      collapsed,
      selectedKeys,
      openKeys,
      handleSubmit,
      goBack,
      showCategoryModal
    };
  }
});
</script>

<style scoped>
.role-create {
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
</style>
