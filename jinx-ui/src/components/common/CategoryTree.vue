<template>
  <div class="category-tree">
    <!-- 已选择的路径展示 -->
    <div v-if="currentLevel == 0" class="selected-path">
      <span class="path-label">已选择：</span>
      <template v-if="isAllSelected">
        <span class="path-item">全部</span>
      </template>
      <template v-else>
        <template v-for="(item, index) in selectedPath" :key="item.id">
          <span class="path-separator" v-if="index > 0">/</span>
          <span class="path-item">{{ item.name }}</span>
        </template>
      </template>
    </div>

    <!-- 当前层级的分类选择 -->
    <div class="category-level">
      <a-button
        class="category-btn"
        :type="isAllSelected ? 'primary' : 'default'"
        @click="handleSelect(0)"
      >
        全部
      </a-button>
      <a-button
        v-for="category in categories"
        :key="category.id"
        class="category-btn"
        :type="isCategorySelected(category.id) ? 'primary' : 'default'"
        @click="handleSelect(category.id)"
      >
        {{ category.name }}
        <CaretUpOutlined v-if="isCategorySelected(category.id)" />
        <CaretDownOutlined v-else />
      </a-button>
    </div>
    <!-- 递归渲染子分类 -->
    <div v-if="!isAllSelected && hasChildren" class="sub-categories">
      <CategoryTree
        :categories="currentChildren"
        :selected-path="selectedPath"
        :current-level="currentLevel + 1"
        @select="handleChildSelect"
      />
    </div>
  </div>
</template>

<script>
import { defineComponent, computed } from 'vue';
import { CaretUpOutlined, CaretDownOutlined } from '@ant-design/icons-vue';

const CategoryTree = defineComponent({
  name: 'CategoryTree',
  components: {
    CaretUpOutlined,
    CaretDownOutlined
  },
  props: {
    categories: {
      type: Array,
      required: true,
      default: () => []
    },
    selectedPath: {
      type: Array,
      default: () => []
    },
    currentLevel: {
      type: Number,
      default: 0
    }
  },
  emits: ['select'],
  setup(props, { emit }) {
    // 计算当前是否选择了"全部"
    const isAllSelected = computed(() => {
      return props.selectedPath.length <= props.currentLevel;
    });

    // 计算当前选中的分类是否有子分类
    const hasChildren = computed(() => {
      if (isAllSelected.value) return false;
      const currentSelected = props.selectedPath[props.currentLevel];
      if (!currentSelected) return false;
      return currentSelected.children && currentSelected.children.length > 0;
    });

    // 获取当前选中分类的子分类
    const currentChildren = computed(() => {
      if (isAllSelected.value) return [];
      const currentSelected = props.selectedPath[props.currentLevel];
      if (!currentSelected) return [];
      return currentSelected.children || [];
    });

    // 判断当前分类是否被选中
    const isCategorySelected = (categoryId) => {
      if (isAllSelected.value) return false;
      const currentSelected = props.selectedPath[props.currentLevel];
      return currentSelected && currentSelected.id === categoryId;
    };

    // 处理分类选择
    const handleSelect = (id) => {
      if (id === 0) {
        // 选择"全部"，截断路径到当前层级
        const newPath = props.selectedPath.slice(0, props.currentLevel);
        emit('select', newPath);
      } else {
        // 选择具体分类，更新当前层级的路径
        const category = props.categories.find(cat => cat.id === id);
        if (category) {
          const newPath = [...props.selectedPath.slice(0, props.currentLevel), category];
          emit('select', newPath);
        }
      }
    };

    // 处理子分类选择
    const handleChildSelect = (path) => {
      emit('select', path);
    };

    return {
      isAllSelected,
      hasChildren,
      currentChildren,
      isCategorySelected,
      handleSelect,
      handleChildSelect
    };
  }
});

export default CategoryTree;
</script>

<style scoped>
.category-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selected-path {
  display: flex;
  align-items: center;
  padding: 8px 0;
  color: #666;
}

.path-label {
  margin-right: 8px;
  font-weight: 500;
}

.path-separator {
  margin: 0 8px;
  color: #999;
}

.path-item {
  color: #1890FF;
  font-weight: 500;
}

.category-level {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-categories {
  margin-top: 8px;
}
</style>