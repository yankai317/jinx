<template>
  <div class="org-user-component">
    <a-spin :spinning="loading">
      <UserAndGroupSelect
        @change="infoChange"
        :preSelectedItems="selectedItems"
        :bizId="bizId"
        :bizType="bizType"
        @success="handleSuccess"
      />

      <div class="selected-items">
        <h4>已选择项</h4>
        <div class="selected-tags">
          <template v-if="selectedItems.length > 0">
            <a-tag v-for="item in selectedItems" :key="`${item.type}-${item.id}`" closable
              @close="handleRemoveItem(item)">
              {{ item.name }} ({{ getTypeName(item.type) }})
            </a-tag>
          </template>
          <template v-else>
            <span class="no-selected">暂无选择</span>
          </template>
        </div>
      </div>

      <div class="component-footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" @click="handleConfirm" style="margin-left: 8px">确定</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, onMounted, defineComponent, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/utils/request';
import UserAndGroupSelect from './UserAndGroupSelect.vue';

export default defineComponent({

  name: 'OrgUserComponent',
  components: {
    UserAndGroupSelect,
  },
  props: {
    // 业务ID
    bizId: {
      type: Number,
      default: null
    },
    // 业务类型
    bizType: {
      type: String,
      default: 'course'
    },
    // 已选择的项目
    selectedIds: {
      type: Array,
      default: () => []
    },
    // 选择类型：'single'(单选) 或 'multiple'(多选)
    selectionType: {
      type: String,
      default: 'multiple'
    },
  },
  setup(props, { emit }) {
    const loading = ref(false);
    const selectedItems = ref([]);

    // 初始化已选项
    const initSelectedItems = () => {
      console.log('initSelectedItems called, selectedIds:', props.selectedIds);
      if (props.selectedIds && props.selectedIds.length > 0) {
        selectedItems.value = [...props.selectedIds];
        console.log('selectedItems initialized:', selectedItems.value);
      }
    };

    /** 范围修改 */
    const infoChange = (value) => {
      console.log('infoChange', value)
      selectedItems.value = value
    }


    // 移除已选择项
    const handleRemoveItem = (item) => {
      selectedItems.value = selectedItems.value.filter(i => !(i.id === item.id && i.type === item.type));
    };


    // 获取类型名称
    const getTypeName = (type) => {
      const typeMap = {
        'department': '部门',
        'role': '角色',
        'user': '人员'
      };
      return typeMap[type] || type;
    };
    // 取消
    const handleCancel = () => {
      emit('cancel');
    };

    // 确认
    const handleConfirm = () => {
      // 返回选择结果
      const result = {
        departments: selectedItems.value
          .filter(item => item.type === 'department')
          .map(item => ({ id: item.id, name: item.name })),
        roles: selectedItems.value
          .filter(item => item.type === 'role')
          .map(item => ({ id: item.id, name: item.name })),
        users: selectedItems.value
          .filter(item => item.type === 'user')
          .map(item => ({ id: item.id, name: item.name })),
        items: selectedItems.value
      };

      emit('confirm', result);
    };

    // 处理选择成功
    const handleSuccess = (result) => {
      if (result && result.items) {
        selectedItems.value = result.items;
      }
    };

    onMounted(() => {
      initSelectedItems();
    });

    // 监听selectedIds变化
    watch(() => props.selectedIds, (newVal) => {
      console.log('selectedIds changed:', newVal);
      if (newVal && newVal.length > 0) {
        selectedItems.value = [...newVal];
        console.log('selectedItems updated from watch:', selectedItems.value);
      }
    }, { deep: true });

    return {
      loading,
      selectedItems,
      infoChange,
      handleRemoveItem,
      getTypeName,
      handleCancel,
      handleConfirm,
      handleSuccess,
    };
  }
});
</script>

<style scoped>
.org-user-component {
  height: calc(100vh - 240px);
  overflow-y: auto;
  padding: 16px;
}

.department-tree-container,
.role-tree-container {
  display: flex;
  gap: 16px;
}

.department-tree-container :deep(.ant-tree),
.role-tree-container :deep(.ant-tree) {
  flex: 1;
  max-width: 300px;
  border-right: 1px solid #f0f0f0;
  padding-right: 16px;
  overflow: auto;
  max-height: 400px;
}

.department-users-container,
.role-users-container {
  flex: 1;
  padding-left: 16px;
  max-height: 400px;
  overflow: auto;
}

.department-users-header,
.role-users-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #f0f0f0;
}

.selected-items {
  margin-top: 24px;
  border-top: 1px dashed #e8e8e8;
  padding-top: 16px;
}

.selected-tags {
  margin-top: 8px;
}

.no-selected {
  color: rgba(0, 0, 0, 0.45);
}

.component-footer {
  margin-top: 24px;
  text-align: right;
}
</style>
