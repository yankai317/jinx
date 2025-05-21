
<template>
  <div class="learning-record-list-page">
    <!-- 顶部导航区域 -->
    <HeaderComponent activeKey="PERSONAL_CENTER" />

    <div class="page-content">
      <div class="page-title">
        <h1>学习记录</h1>
        <p>查看您的所有学习记录，跟踪学习进度</p>
      </div>

      <!-- 筛选区域 -->
      <div class="filter-section">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="记录类型">
              <a-select
                v-model:value="filters.type"
                placeholder="选择记录类型"
                @change="handleFilterChange"
              >
                <a-select-option value="all">全部</a-select-option>
                <a-select-option value="course">课程</a-select-option>
                <a-select-option value="train">培训</a-select-option>
                <a-select-option value="map">学习地图</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="完成状态">
              <a-select
                v-model:value="filters.status"
                placeholder="选择完成状态"
                @change="handleFilterChange"
              >
                <a-select-option :value="0">全部</a-select-option>
                <a-select-option :value="1">学习中</a-select-option>
                <a-select-option :value="2">已完成</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="时间范围">
              <a-range-picker
                v-model:value="dateRange"
                format="YYYY-MM-DD"
                :placeholder="['开始日期', '结束日期']"
                @change="handleDateRangeChange"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 记录列表区域 -->
      <div class="record-list-section">
        <a-spin :spinning="loading">
          <a-list
            v-if="records.length > 0"
            :data-source="records"
            :pagination="false"
            item-layout="horizontal"
          >
            <template #renderItem="{ item }">
              <a-list-item @click="handleRecordClick(item)">
                <div class="record-item">
                  <div class="record-cover">
                    <img :src="item.coverUrl || 'https://via.placeholder.com/120x80'" alt="封面图" />
                    <div class="record-type-tag">
                      <a-tag :color="getTypeTagColor(item.contentType)">
                        {{ getTypeTagText(item.contentType) }}
                      </a-tag>
                    </div>
                  </div>
                  <div class="record-content">
                    <div class="record-title">{{ item.contentName }}</div>
                    <div class="record-progress">
                      <div class="progress-label">
                        <span>学习进度</span>
                        <span>{{ item.progress }}%</span>
                      </div>
                      <a-progress
                        :percent="item.progress"
                        :status="item.status === 1 ? 'success' : 'active'"
                        :stroke-color="item.status === 1 ? '#52c41a' : '#1890ff'"
                      />
                    </div>
                    <div class="record-info">
                      <div class="info-item">
                        <clock-circle-outlined />
                        <span>学习时长: {{ item.studyDuration }}分钟</span>
                      </div>
                      <div class="info-item">
                        <calendar-outlined />
                        <span>开始学习: {{ formatDate(item.startTime) }}</span>
                      </div>
                      <div class="info-item" v-if="item.lastStudyTime">
                        <history-outlined />
                        <span>最后学习: {{ formatDate(item.lastStudyTime) }}</span>
                      </div>
                      <div class="info-item" v-if="item.completionTime">
                        <check-circle-outlined />
                        <span>完成时间: {{ formatDate(item.completionTime) }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="record-status">
                    <a-tag :color="item.status === 1 ? 'success' : 'processing'">
                      {{ item.status === 1 ? '已完成' : '学习中' }}
                    </a-tag>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>

          <!-- 空状态展示 -->
          <div v-else class="empty-container">
            <EmptyState
              title="暂无学习记录"
              description="您还没有任何学习记录，去学习中心开始学习吧"
            >
              <template #action>
                <a-button type="primary" @click="goToLearningCenter">
                  前往学习中心
                </a-button>
              </template>
            </EmptyState>
          </div>
        </a-spin>
      </div>

      <!-- 分页区域 -->
      <div class="pagination-section" v-if="total > 0">
        <a-pagination
          v-model:current="pagination.current"
          :total="total"
          :pageSize="pagination.pageSize"
          show-size-changer
          :pageSizeOptions="['10', '20', '50', '100']"
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
          show-total="(total) => `共 ${total} 条记录`"
        />
      </div>
    </div>

    <!-- 底部区域 -->
    <FooterComponent />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ClockCircleOutlined,
  CalendarOutlined,
  HistoryOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserLearningRecords } from '@/api/toc/personal';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'LearningRecordListPage',
  components: {
    HeaderComponent,
    FooterComponent,
    EmptyState,
    ClockCircleOutlined,
    CalendarOutlined,
    HistoryOutlined,
    CheckCircleOutlined
  },
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const records = ref([]);
    const total = ref(0);
    const dateRange = ref([]);

    // 筛选条件
    const filters = reactive({
      type: 'all',
      status: 0,
      startTime: '',
      endTime: ''
    });

    // 分页信息
    const pagination = reactive({
      current: 1,
      pageSize: 10
    });

    // 获取学习记录
    const fetchRecords = async () => {
      loading.value = true;
      try {
        const params = {
          type: filters.type,
          status: filters.status,
          startTime: filters.startTime,
          endTime: filters.endTime,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        };

        const response = await getUserLearningRecords(params);
        if (response.code === 200 && response.data) {
          records.value = response.data.list || [];
          total.value = response.data.total || 0;
        } else {
          message.error(response.message || '获取学习记录失败');
        }
      } catch (error) {
        console.error('获取学习记录出错:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理筛选条件变化
    const handleFilterChange = () => {
      pagination.current = 1;
      fetchRecords();
    };

    // 处理日期范围变化
    const handleDateRangeChange = (dates, dateStrings) => {
      filters.startTime = dateStrings[0] || '';
      filters.endTime = dateStrings[1] || '';
      handleFilterChange();
    };

    // 处理页码变化
    const handlePageChange = (page, pageSize) => {
      pagination.current = page;
      pagination.pageSize = pageSize;
      fetchRecords();
    };

    // 处理每页条数变化
    const handlePageSizeChange = (current, size) => {
      pagination.current = 1;
      pagination.pageSize = size;
      fetchRecords();
    };

    // 处理记录点击
    const handleRecordClick = (record) => {
      if (record.contentType === 'course') {
        router.push(`/toc/course/${record.contentId}`);
      } else if (record.contentType === 'train') {
        router.push(`/toc/train/${record.contentId}`);
      } else if (record.contentType === 'map') {
        router.push(`/toc/map/${record.contentId}`);
      }
    };

    // 前往学习中心
    const goToLearningCenter = () => {
      router.push('/toc/learning/center');
    };

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      return dayjs(dateString).format('YYYY-MM-DD');
    };

    // 获取类型标签文本
    const getTypeTagText = (type) => {
      switch (type) {
        case 'course':
          return '课程';
        case 'train':
          return '培训';
        case 'map':
          return '学习地图';
        default:
          return '未知';
      }
    };

    // 获取类型标签颜色
    const getTypeTagColor = (type) => {
      switch (type) {
        case 'course':
          return 'blue';
        case 'train':
          return 'purple';
        case 'map':
          return 'green';
        default:
          return 'default';
      }
    };

    onMounted(() => {
      fetchRecords();
    });

    return {
      loading,
      records,
      total,
      filters,
      pagination,
      dateRange,
      handleFilterChange,
      handleDateRangeChange,
      handlePageChange,
      handlePageSizeChange,
      handleRecordClick,
      goToLearningCenter,
      formatDate,
      getTypeTagText,
      getTypeTagColor
    };
  }
});
</script>

<style scoped>
.learning-record-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.page-content {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}

.page-title {
  margin-bottom: 24px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-title p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.filter-section {
  background-color: #ffffff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.record-list-section {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  min-height: 400px;
}

.record-item {
  display: flex;
  width: 100%;
  padding: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.record-item:hover {
  background-color: #f9fafb;
}

.record-cover {
  position: relative;
  width: 160px;
  height: 90px;
  margin-right: 16px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
}

.record-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.record-type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.record-content {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-progress {
  margin-bottom: 12px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  color: #6b7280;
}

.record-info {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}

.info-item .anticon {
  margin-right: 4px;
}

.record-status {
  display: flex;
  align-items: flex-start;
  margin-left: 16px;
}

.pagination-section {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.empty-container {
  padding: 48px 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .record-item {
    flex-direction: column;
  }

  .record-cover {
    width: 100%;
    height: 120px;
    margin-right: 0;
    margin-bottom: 16px;
  }

  .record-status {
    margin-left: 0;
    margin-top: 16px;
  }
}
</style>
