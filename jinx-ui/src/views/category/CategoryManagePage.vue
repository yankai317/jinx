<template>
  <a-layout class="certificate-create-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <PageBreadcrumb />
        </div>

        <a-card class="form-card">
          <div class="category-header">
            <div class="header-title">
              <span>分类管理</span>
            </div>
            <div class="header-actions">
              <a-button type="primary" @click="addRootCategory" :disabled="isEditing || isSorting">
                <plus-outlined />添加一级类目
              </a-button>
              <a-button v-if="!isSorting" @click="startSorting" :disabled="isEditing">
                <drag-outlined />排序
              </a-button>
              <a-button v-else @click="cancelSorting">
                <close-outlined />放弃排序
              </a-button>
              <a-button v-if="isSorting" type="primary" @click="saveSorting">
                <save-outlined />保存排序
              </a-button>
            </div>
          </div>

          <a-spin :spinning="loading">
            <div class="tree-container">
              <a-empty v-if="treeData.length === 0" description="暂无分类数据" />
              <a-tree v-else class="category-tree" :tree-data="treeData" :selectable="true" :draggable="isSorting"
                :blockNode="true" :showIcon="true" show-line :selectedKeys="selectedKeys" @select="onSelect"
                @drop="onDrop" :expandAction="false" @expand="onExpand" :expandedKeys="expandedKeys"
                :autoExpandParent="autoExpandParent" :fieldNames="{ title: 'name', key: 'id', children: 'children' }">
                <template #title="{ name, id }">
                  <div class="tree-node-title">
                    <div class="node-content">
                      <span v-if="editingId !== id">{{ name }}</span>
                      <template v-else>
                        <a-input v-model:value="editingName" size="small" style="width: 200px" maxlength="30"
                          placeholder="最多30字" />
                        <a-button type="text" size="small" @click.stop="cancelEdit(id)" title="取消">
                          <close-outlined />
                        </a-button>
                        <a-button type="text" size="small" @click.stop="saveEdit(id)" title="保存">
                          <save-outlined />
                        </a-button>
                      </template>

                    </div>
                    <div class="node-actions" v-if="!isSorting">
                      <a-button type="text" size="small" @click.stop="renameCategory(id, name)"
                        :disabled="isEditing && editingId !== id" title="重命名">
                        <edit-outlined />
                      </a-button><a-button type="text" size="small" @click.stop="addChildCategory(id)"
                        :disabled="isEditing" title="添加子类目">
                        <plus-outlined />
                      </a-button>
                      <a-popconfirm title="确定要删除此类目吗？" description="删除后将同时删除所有子类目，且无法恢复！类目删除后所有的内容与该类目、子类目的绑定关系也会删除。"
                        @confirm="confirmDeleteCategory(id)" @cancel="cancelDelete" ok-text="确定"
                        cancel-text="取消"><a-button type="text" size="small" danger @click.stop :disabled="isEditing"
                          title="删除">
                          <delete-outlined />
                        </a-button></a-popconfirm>
                    </div>
                  </div>
                </template>
                <template #switcherIcon="{ defaultIcon }">
                  <span v-if="isSorting" class="drag-handle">
                    <menu-outlined />
                  </span>
                  <component :is="defaultIcon" v-else />
                </template>
              </a-tree>
            </div>
          </a-spin>
        </a-card>
      </a-layout-content>
    </a-layout>

    <!-- 类目管理弹窗 -->
    <a-modal v-model:visible="categoryModalVisible" title="类目管理" width="800px" :footer="null" destroyOnClose>
      <category-management v-if="categoryModalVisible" /></a-modal>
  </a-layout>
</template>

<script>
import { ref, reactive, computed, watch, defineComponent } from 'vue';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileOutlined,
  DragOutlined,
  SaveOutlined,
  CloseOutlined,
  PlusSquareOutlined,
  MinusSquareOutlined,
  MenuOutlined
} from '@ant-design/icons-vue';
import { getCategoryList, createCategory, updateCategory, deleteCategory, updateCategorySort } from '@/api/category';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import { onMounted } from 'vue';

export default defineComponent({
  name: 'CategoryManagePage',
  components: {
    HeaderComponent,
    SiderComponent,
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    FileOutlined,
    DragOutlined,
    SaveOutlined,
    CloseOutlined,
    PlusSquareOutlined,
    MinusSquareOutlined,
    MenuOutlined,
    PageBreadcrumb
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'refresh'],
  setup(props, { emit }) {
    const collapsed = ref(false);
    const selectedKeys = ref(['category']);
    const openKeys = ref(['operation']);
    // 数据状态
    const loading = ref(false);
    const treeData = ref([]);
    const expandedKeys = ref([]);
    const autoExpandParent = ref(true);
    const editingId = ref(null);
    const editingName = ref('');
    const isEditing = ref(false);
    const isSorting = ref(false);
    const originalTreeData = ref([]);


    // 获取类目列表
    const fetchCategories = async () => {
      loading.value = true;
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          treeData.value = res.data || [];
          originalTreeData.value = res.data || [];
        } else {
          message.error(res.message || '获取类目列表失败');
        }
      } catch (error) {
        console.error('获取类目列表失败:', error);
      } finally {
        loading.value = false;
      }
    };


    // 展开/收起节点
    const onExpand = (keys, info) => {
      expandedKeys.value = keys;
      autoExpandParent.value = false;
    };

    // 选择节点
    const onSelect = (keys, info) => {
      if (isEditing.value && editingId.value !== info.node.key) {
        message.warning('请先完成当前编辑操作');
        return;
      }
      selectedKeys.value = keys;
    };

    // 添加一级类目
    const addRootCategory = () => {
      if (isEditing.value) {
        message.warning('请先完成当前编辑操作');
        return;
      }

      editingId.value = 'new-root';
      editingName.value = '';
      isEditing.value = true;

      // 添加一个临时节点
      treeData.value.push({
        id: 'new-root',
        title: '',
        parentId: 0,
        isLeaf: true,
        children: []
      });

      // 选中新节点
      selectedKeys.value = ['new-root'];
    };

    // 添加子类目
    const addChildCategory = (parentId) => {
      if (isEditing.value) {
        message.warning('请先完成当前编辑操作');
        return;
      }

      const newId = `new-${parentId}`;
      editingId.value = newId;
      editingName.value = '';
      isEditing.value = true;

      // 查找父节点
      const findAndAddChild = (nodes) => {
        for (let i = 0; i < nodes.length; i++) {
          if (nodes[i].id === parentId) {
            // 确保children数组存在
            if (!nodes[i].children) {
              nodes[i].children = [];
            }

            // 添加临时子节点
            nodes[i].children.push({
              id: newId,
              name: '',
              parentId: parentId,
              isLeaf: true,
              children: []
            });

            // 确保父节点不是叶子节点
            nodes[i].isLeaf = false;

            // 自动展开父节点
            if (!expandedKeys.value.includes(parentId)) {
              expandedKeys.value = [...expandedKeys.value, parentId];
              autoExpandParent.value = true;
            }

            return true;
          }

          if (nodes[i].children && nodes[i].children.length > 0) {
            if (findAndAddChild(nodes[i].children)) {
              return true;
            }
          }
        }

        return false;
      };

      findAndAddChild(treeData.value);

      // 选中新节点
      selectedKeys.value = [newId];
    };

    // 重命名类目
    const renameCategory = (key, name) => {
      if (isEditing.value && editingId.value !== key) {
        message.warning('请先完成当前编辑操作');
        return;
      }

      editingId.value = key;
      editingName.value = name;
      isEditing.value = true;
    };

    // 递归查找所在父节点
    const findParentNode = (nodes, id) => {
      for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].id === id) {
          return nodes;
        }
        if (nodes[i].children && nodes[i].children.length > 0) {
          const result = findParentNode(nodes[i].children, id);
          if (result) {
            return result;
          }
        }
      }
    }

    // 取消编辑
    const cancelEdit = (id) => {
      const parentNode = findParentNode(treeData.value, id);

      console.log(parentNode);
      // 删除临时节点
      if (editingId.value.toString().startsWith('new-')) {
        parentNode.splice(parentNode.findIndex(item => item.id === id), 1);
      }
      editingId.value = null;
      editingName.value = '';
      isEditing.value = false;
    };

    // 保存编辑
    const saveEdit = async (key) => {
      if (!editingName.value || editingName.value.trim() === '') {
        message.error('类目名称不能为空');
        return;
      }

      if (editingName.value.length > 30) {
        message.error('类目名称不能超过30个字符');
        return;
      }

      loading.value = true;

      try {
        if (key === 'new-root') {
          // 创建一级类目
          const res = await createCategory({
            name: editingName.value,
            parentId: 0,
            sort: 0
          });

          if (res.code === 200) {
            message.success('添加类目成功');
            await fetchCategories(); // 重新获取类目列表
          } else {
            message.error(res.message || '添加类目失败');
          }
        } else if (key.toString().startsWith('new-')) {
          // 创建子类目
          const parentId = parseInt(key.split('-')[1]);
          const res = await createCategory({
            name: editingName.value,
            parentId: parentId,
            sort: 0
          });

          if (res.code === 200) {
            message.success('添加类目成功');
            await fetchCategories(); // 重新获取类目列表
          } else {
            message.error(res.message || '添加类目失败');
          }
        } else {
          // 更新类目名称
          const res = await updateCategory({
            id: key,
            name: editingName.value
          });

          if (res.code === 200) {
            message.success('更新类目成功');
            await fetchCategories(); // 重新获取类目列表
          } else {
            message.error(res.message || '更新类目失败');
            await fetchCategories(); // 重新获取类目列表
          }
        }
      } catch (error) {
        console.error('保存类目失败:', error);
      } finally {
        // 清除编辑状态
        editingId.value = null;
        editingName.value = '';
        isEditing.value = false;
        loading.value = false;
      }
    };

    // 删除类目
    const confirmDeleteCategory = async (key) => {
      loading.value = true;

      try {
        const res = await deleteCategory(key);

        if (res.code === 200) {
          message.success('删除类目成功');
          await fetchCategories(); // 重新获取类目列表
          selectedKeys.value = []; // 清除选中状态
        } else {
          message.error(res.message || '删除类目失败');
        }
      } catch (error) {
        console.error('删除类目失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 取消删除
    const cancelDelete = () => {
      // 不做任何操作
    };

    // 开始排序
    const startSorting = () => {
      if (isEditing.value) {
        message.warning('请先完成当前编辑操作');
        return;
      }

      isSorting.value = true;
      message.info('现在可以拖拽类目进行排序，支持拖拽到任一位置和层级');
    };

    // 取消排序
    const cancelSorting = () => {
      isSorting.value = false;// 恢复原始数据
      treeData.value = JSON.parse(JSON.stringify(originalTreeData.value));
    };

    /** 递归更新排序 */
    const updateSort = (nodes) => {
      for (let i = 0; i < nodes.length; i++) {
        nodes[i].sort = i;
        if (nodes[i].children && nodes[i].children.length > 0) {
          updateSort(nodes[i].children);
        }
      }
      return nodes;
    };

    /** 保存排序 */
    const saveSorting = async () => {
      loading.value = true;

      try {
        const sortedData = updateSort(treeData.value);
        console.log(sortedData);
        await updateCategorySort(sortedData);

        message.success('保存排序成功');
        isSorting.value = false;
        // 重新获取类目列表，确保数据一致
        await fetchCategories();
      } catch (error) {
        console.error('保存排序失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理拖拽
    const onDrop = (info) => {
      console.log(info);
      const dropId = info.node.id;
      const dragId = info.dragNode.id;
      const dropPos = info.node.pos.split('-');
      const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
      const loop = (data, id, callback) => {
        data.forEach((item, index) => {
          if (item.id === id) {
            return callback(item, index, data);
          }
          if (item.children) {
            return loop(item.children, id, callback);
          }
        });
      };
      const data = JSON.parse(JSON.stringify(treeData.value));

      // Find dragObject
      let dragObj;
      loop(data, dragId, (item, index, arr) => {
        arr.splice(index, 1);
        dragObj = item;
      });
      if (!info.dropToGap) {
        // Drop on the content
        loop(data, dropId, (item) => {
          item.children = item.children || [];
          /// where to insert 示例添加到头部，可以是随意位置
          item.children.unshift(dragObj);
        });
      } else if (
        (info.node.children || []).length > 0 && // Has children
        info.node.expanded && // Is expanded
        dropPosition === 1 // On the bottom gap
      ) {
        loop(data, dropId, (item) => {
          item.children = item.children || [];
          // where to insert 示例添加到头部，可以是随意位置
          item.children.unshift(dragObj);
        });
      } else {
        let ar = [];
        let i = 0;
        loop(data, dropId, (_item, index, arr) => {
          ar = arr;
          i = index;
        });
        if (dropPosition === -1) {
          ar.splice(i, 0, dragObj);
        } else {
          ar.splice(i + 1, 0, dragObj);
        }
      }

      treeData.value = data;
    };

    // 处理取消
    const handleCancel = () => {
      if (isEditing.value) {
        message.warning('请先完成当前编辑操作');
        return;
      }

      if (isSorting.value) {
        message.warning('请先完成或取消排序操作');
        return;
      }

      emit('update:visible', false);
    };

    // 监听visible变化
    watch(() => props.visible, (val) => {
      if (val) {
        fetchCategories();
      }
    });

    onMounted(() => {
      fetchCategories()
    })

    return {
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      treeData,
      selectedKeys,
      expandedKeys,
      autoExpandParent,
      editingId,
      editingName,
      isEditing,
      isSorting,
      onSelect,
      onExpand,
      addRootCategory,
      addChildCategory,
      renameCategory,
      saveEdit,
      cancelEdit,
      confirmDeleteCategory,
      cancelDelete,
      startSorting,
      cancelSorting,
      saveSorting,
      onDrop,
      handleCancel
    };
  }
});
</script>

<style scoped>
.category-management {
  width: 100%;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.content-header {
  padding-bottom: 12px;
}

.category-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-title {
  font-size: 16px;
  font-weight: bold;
}

.operation-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.operation-desc p {
  margin-bottom: 4px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-self: flex-start;
}

.tree-container {
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 2px;
  padding: 8px;
}

.category-tree {
  width: 100%;
}

.tree-node-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 8px;
}

.node-content {
  flex: 1;
}

.node-actions {
  display: flex;
  margin-left: 16px;
}

:deep(.ant-tree-node-content-wrapper) {
  width: 100%;
  transition: background-color 0.3s;
}

:deep(.ant-tree-node-content-wrapper:hover) {
  background-color: #f5f5f5 !important;
}

:deep(.ant-tree-node-selected) {
  background-color: #e6f7ff !important;
}

:deep(.ant-tree-treenode) {
  width: 100%;
  padding: 4px 0 !important;
}

:deep(.ant-tree-switcher) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.ant-tree-treenode-draggable) {
  cursor: move;
}

:deep(.ant-tree-treenode-dragging) {
  background-color: #f0f7ff;
  opacity: 0.8;
}

.drag-handle {
  color: #999;
  margin-right: 4px;
  cursor: move;
}

:deep(.ant-tree-draggable-icon) {
  cursor: move !important;
}
</style>
