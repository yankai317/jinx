<template>
  <div class="category-setting">
    <a-spin :spinning="loading">
      <div class="category-header">
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>

      <a-form :model="categoryForm" layout="vertical">
        <a-form-item label="选择分类">
          <CategoryTreeSelect v-model:value="categoryForm.selectedCategories" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" @click="handleSave">保存</a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, onMounted, defineComponent, computed } from 'vue';
import { message, TreeSelect } from 'ant-design-vue';
import { changeCategory } from '@/api/common';
import dayjs from 'dayjs';
import { getCategoryList } from '@/api/category';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
// 初始化dayjs中文
dayjs.locale('zh-cn');

export default defineComponent({
  name: 'categorySettingComponet',
  components: {
    CategoryTreeSelect
  },
  props: {
    // 业务模块ID
    bizId: {
      type: Number,
      required: true
    },
    // 业务模块类型：course, train, learningMap
    bizType: {
      type: String,
      required: true,
      validator: (value) => {
        return ['COURSE', 'TRAIN', 'LEARNING_MAP'].includes(value);
      }
    },
    // 当前已选择的分类ID数组
    currentCategories: {
      type: Array,
      default: () => []
    },
    // 标题
    title: {
      type: String,
      default: '分类设置'
    },
    // 描述
    description: {
      type: String,
      default: '选择合适的分类，可多选'
    },
    // 保存成功回调
    onSuccess: {
      type: Function,
      default: () => {}
    },
    // 取消回调
    onCancel: {
      type: Function,
      default: () => {}
    },
    // 自定义保存方法
    customSave: {
      type: Function,
      default: null
    }
  },
  setup(props) {
    const loading = ref(false);
    const treeSelectRef = ref(null);
    const SHOW_PARENT = TreeSelect.SHOW_PARENT;

    // 分类表单
    const categoryForm = reactive({
      selectedCategories: props.currentCategories || []
    });

    // 分类树
    const categoryTree = ref([]);

    // 获取分类列表
    const fetchCategories = async () => {
      loading.value = true;
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          categoryTree.value = formatCategoryTree(res.data || []);
        } else {
          message.error(res.message || '获取分类列表失败');
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 格式化分类树数据
    const formatCategoryTree = (categories) => {
      const rootCategories = categories.filter(cat => cat.parentId === 0);

      const buildTree = (category) => {
        const children = categories.filter(cat => cat.parentId === category.id);

        return {
          title: category.name,
          value: category.id,
          key: category.id,
          children: children.length > 0 ? children.map(buildTree) : undefined
        };
      };

      return rootCategories.map(buildTree);
    };

    // 保存分类设置
    const handleSave = async () => {
      loading.value = true;
      try {
        // 如果提供了自定义保存方法，则使用自定义方法
        if (props.customSave) {
          await props.customSave(categoryForm.selectedCategories);
          message.success('分类设置成功');
          props.onSuccess(categoryForm.selectedCategories);
          return;
        }


        const res = await changeCategory({
          bizId: props.bizId,
          bizType: props.bizType,
          categoryIds: categoryForm.selectedCategories
        });

        if (res && res.code === 200) {
          message.success('分类设置成功');
          props.onSuccess(categoryForm.selectedCategories);
        } else {
          message.error((res && res.message) || '分类设置失败');
        }
      } catch (error) {
        console.error('分类设置失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 取消设置
    const handleCancel = () => {
      props.onCancel();
    };

    onMounted(() => {
      // 获取分类列表
      fetchCategories();

      // 设置当前选中的分类
      if (props.currentCategories && props.currentCategories.length > 0) {
        categoryForm.selectedCategories = props.currentCategories;
      }
    });

    return {
      loading,
      treeSelectRef,
      categoryForm,
      categoryTree,
      SHOW_PARENT,
      handleSave,
      handleCancel
    };
  }
});
</script>

<style scoped>
.category-setting {
  padding: 16px;
}

.category-header {
  margin-bottom: 16px;
}

.category-header h3 {
  margin-bottom: 8px;
}

.category-header p {
  color: rgba(0, 0, 0, 0.45);
}
</style>
