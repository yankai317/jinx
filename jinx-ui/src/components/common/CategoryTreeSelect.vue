<template>
  <a-tree-select
    v-model:value="selectedValue"
    :tree-data="treeData"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :multiple="multiple"
    :field-names="fieldNames"
    change-on-select
    style="width: 100%"
    @change="handleChange"
    v-bind="$attrs"
  />
</template>

<script>
import { ref, watch, defineComponent } from 'vue';
import { getCategoryList } from '@/api/category';

export default defineComponent({
  name: 'CategoryTreeSelect',
  props: {
    value: {
      type: [String, Number, Array] || String,
      default: undefined
    },
    placeholder: {
      type: String,
      default: '请选择分类'
    },
    allowClear: {
      type: Boolean,
      default: true
    },
    multiple: {
      type: Boolean,
      default: false
    },
    fieldNames: {
      type: Object,
      default: () => ({
        label: 'name',
        value: 'id'
      })
    }
  },
  emits: ['update:value', 'change'],
  setup(props, { emit }) {
    const selectedValue = ref(props.value);
    const treeData = ref([]);

    // 获取分类列表并转换为树形结构
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200 && res.data) {
          treeData.value = res.data;
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      }
    };

    // 处理选择变更
    const handleChange = (value) => {
      emit('update:value', value);
      emit('change', value);
    };

    // 监听外部value变化
    watch(() => props.value, (newVal) => {
      selectedValue.value = newVal;
    });

    // 初始化时获取分类数据
    fetchCategories();

    return {
      selectedValue,
      treeData,
      handleChange
    };
  }
});
</script>

<style scoped>
/* 可以添加自定义样式 */
</style>