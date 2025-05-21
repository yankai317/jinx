<template>
  <div>
    <a-modal
      :open="visible"
      :title="modalTitle"
      :width="700"
      :footer="null"
      @cancel="handleCancel"
    >
      <div class="course-relate">
        <a-spin :spinning="loading">

          <div class="filter-container" v-show="showFilter">
            <span class="type-label">类型:</span>
            <a-checkbox-group v-model:value="selectedTypes" @change="handleFilterChange">
              <a-checkbox value="TRAIN" :disabled="isTypeDisabled('TRAIN')">培训</a-checkbox>
              <a-checkbox value="LEARNING_MAP" :disabled="isTypeDisabled('LEARNING_MAP')">学习地图</a-checkbox>
            </a-checkbox-group>
          </div>

          <div class="relate-content">
            <a-empty v-if="filteredRelateList.length === 0" description="暂无关联内容" />
            <div v-else class="relate-list-container">
              <a-table
                :dataSource="filteredRelateList"
                :columns="columns"
                :pagination="false"
                :scroll="{ y: 300 }"
                rowKey="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'type'">
                    <a-tag :color="record.type === 'TRAIN' ? 'blue' : 'green'">
                      {{ record.type === 'TRAIN' ? '培训' : '学习地图' }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
            </div>
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, computed, defineComponent, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import request from '@/utils/request';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'CourseRelateComponent',
  props: {
    bizId: {
      type: Number,
      required: true
    },
    bizType: {
      type: String,
      required: true
    },
    visible: {
      type: Boolean,
      default: false
    },
    showFilter: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:visible', 'cancel'],
  setup(props, { emit }) {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const loading = ref(false);
    const selectedTypes = ref(['TRAIN', 'LEARNING_MAP']); // 默认全选
    const relateList = ref([]);
    const courseName = ref(''); // 存储当前课程名称

    // 模态框标题
    const modalTitle = computed(() => {
      return `查看关联项：${courseName.value}`;
    });

    // 表格列定义
    const columns = [
      {
        title: '名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: 100
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 120
      }
    ];

    // 根据选中的类型过滤关联列表
    const filteredRelateList = computed(() => {
      if (selectedTypes.value.length === 0) {
        return []; // 理论上不会发生，因为至少会有一个选中
      }
      return relateList.value.filter(item => selectedTypes.value.includes(item.type));
    });

    // 判断类型是否应该被禁用（当只剩一个选中时）
    const isTypeDisabled = (type) => {
      return selectedTypes.value.length === 1 && selectedTypes.value.includes(type);
    };

    // 获取课程关联内容
    const fetchRelateContent = async () => {
      loading.value = true;
      try {
        const { getRelatedContent } = require('@/api/course');
        // 获取课程关联内容
        const relateRes = await getRelatedContent({ type: props.bizType, bizId: props.bizId});
        if (relateRes.code === 200) {
          relateList.value = relateRes.data || [];
        } else {
          message.error(relateRes.message || '获取关联内容失败');
        }
      } catch (error) {
        console.error('获取课程关联内容失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理筛选变化
    const handleFilterChange = (checkedValues) => {
      // 确保至少有一个选中
      if (checkedValues.length === 0) {
        // 如果用户尝试取消所有选项，恢复到之前的选择
        selectedTypes.value = [...selectedTypes.value];
        message.warning('至少需要选择一种类型');
      }
    };

    // 处理取消
    const handleCancel = () => {
      emit('update:visible', false);
      emit('cancel');
    };

    // 监听弹窗可见性变化
    watch(() => props.visible, (newVal) => {
      if (newVal) {
        fetchRelateContent();
      }
    });

    return {
      loading,
      selectedTypes,
      relateList,
      filteredRelateList,
      columns,
      handleFilterChange,
      handleCancel,
      isTypeDisabled,
      modalTitle,
      courseName
    };
  }
});
</script>

<style scoped>
.course-relate {
  padding: 0 16px;
}

.relate-header {
  margin-bottom: 16px;
}

.relate-header p {
  color: rgba(0, 0, 0, 0.45);
}

.filter-container {
  margin-bottom: 16px;
}

.type-label {
  margin-right: 8px;
  font-weight: 500;
}

.relate-list-container {
  max-height: 350px;
  overflow-y: auto;
}
</style>
