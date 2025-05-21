<template>
  <div class="learning-record-mobile-page">
    <div class="page-content">
      <div class="page-description">
        <p>查看您的所有学习记录，跟踪学习进度</p>
      </div>

      <!-- 筛选区域 -->
      <div class="filter-section">
        <div class="filter-row">
          <a-select
            v-model:value="filters.type"
            placeholder="记录类型"
            style="width: 100%"
            @change="handleFilterChange"
          >
            <a-select-option value="all">全部类型</a-select-option>
            <a-select-option value="course">课程</a-select-option>
            <a-select-option value="train">培训</a-select-option>
            <a-select-option value="map">学习地图</a-select-option>
          </a-select>
        </div>
        <div class="filter-row">
          <a-select
            v-model:value="filters.status"
            placeholder="完成状态"
            style="width: 100%"
            @change="handleFilterChange"
          >
            <a-select-option :value="0">全部状态</a-select-option>
            <a-select-option :value="1">学习中</a-select-option>
            <a-select-option :value="2">已完成</a-select-option>
          </a-select>
        </div>
        <div class="filter-row">
          <a-range-picker
            v-model:value="dateRange"
            format="YYYY-MM-DD"
            :placeholder="['开始日期', '结束日期']"
            @change="handleDateRangeChange"
            style="width: 100%"
          />
        </div>
      </div>

      <!-- 记录列表区域 -->
      <div class="record-list-section">
        <a-spin :spinning="loading">
          <div v-if="records.length > 0" class="record-list">
            <div
              v-for="item in records"
              :key="item.id"
              class="record-item"
              @click="handleRecordClick(item)"
            >
              <div class="record-header">
                <div class="record-cover">
                  <img :src="item.coverUrl || 'https://via.placeholder.com/120x80'" alt="封面图" />
                  <div class="record-type-tag">
                    <a-tag :color="getTypeTagColor(item.contentType)">
                      {{ getTypeTagText(item.contentType) }}
                    </a-tag>
                  </div>
                </div>
                <div class="record-title-area">
                  <div class="record-title">{{ item.contentName }}</div>
                  <div class="record-status">
                    <a-tag :color="item.status === 1 ? 'success' : 'processing'">
                      {{ item.status === 1 ? '已完成' : '学习中' }}
                    </a-tag>
                  </div>
                </div>
              </div>

              <div class="record-progress">
                <div class="progress-label">
                  <span>学习进度</span>
                  <span>{{ item.progress }}%</span>
                </div>
                <a-progress
                  :percent="item.progress"
                  :status="item.status === 1 ? 'success' : 'active'"
                  :stroke-color="item.status === 1 ? '#52c41a' : '#1890ff'"
                  size="small"
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
          </div>

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
          @change="handlePageChange"
          simple
        />
      </div>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="profile" />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ClockCircleOutlined,
  CalendarOutlined,
  HistoryOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserLearningRecords } from '@/api/toc/personal';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'LearningRecordMobileListPage',
  components: {
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
    const fetchLearningRecords = async () => {
      loading.value = true;
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.pageSize,
          contentType: filters.type === 'all' ? undefined : filters.type,
          status: filters.status === 0 ? undefined : filters.status,
          startTime: filters.startTime,
          endTime: filters.endTime
        };

        const res = await getUserLearningRecords(params);
        if (res.code === 200 && res.data) {
          records.value = res.data.list || [];
          total.value = res.data.total || 0;
        } else {
          message.error(res.message || '获取学习记录失败');
        }
      } catch (error) {
        console.error('获取学习记录异常:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理筛选条件变更
    const handleFilterChange = () => {
      pagination.current = 1;
      fetchLearningRecords();
    };

    // 处理日期范围变更
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        filters.startTime = dates[0] ? dayjs(dates[0]).format('YYYY-MM-DD') : '';
        filters.endTime = dates[1] ? dayjs(dates[1]).format('YYYY-MM-DD') : '';
      } else {
        filters.startTime = '';
        filters.endTime = '';
      }
      pagination.current = 1;
      fetchLearningRecords();
    };

    // 处理页码变更
    const handlePageChange = (page) => {
      pagination.current = page;
      fetchLearningRecords();
    };

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      return dayjs(dateStr).format('YYYY-MM-DD HH:mm');
    };

    // 获取类型标签颜色
    const getTypeTagColor = (type) => {
      const colorMap = {
        'course': 'blue',
        'train': 'green',
        'map': 'purple',
        'exam': 'orange'
      };
      return colorMap[type] || 'default';
    };

    // 获取类型标签文本
    const getTypeTagText = (type) => {
      const textMap = {
        'course': '课程',
        'train': '培训',
        'map': '学习地图',
        'exam': '考试'
      };
      return textMap[type] || type;
    };

    // 处理记录点击
    const handleRecordClick = (record) => {
      const typeRouteMap = {
        'course': `/toc/mobile/course/${record.contentId}`,
        'train': `/toc/mobile/train/${record.contentId}`,
        'map': `/toc/mobile/map/${record.contentId}`,
        'exam': `/toc/mobile/exam/${record.contentId}`
      };

      const route = typeRouteMap[record.contentType];
      if (route) {
        router.push(route);
      } else {
        message.warning('暂不支持查看此类型的详情');
      }
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/mobile/learning/center');
    };

    onMounted(() => {
      fetchLearningRecords();
    });

    return {
      loading,
      records,
      total,
      filters,
      pagination,
      dateRange,
      formatDate,
      getTypeTagColor,
      getTypeTagText,
      handleFilterChange,
      handleDateRangeChange,
      handlePageChange,
      handleRecordClick,
      goToLearningCenter
    };
  }
});
</script>

<style scoped>
.learning-record-mobile-page {
  padding-bottom: 65px;
}

.page-content {
  padding: 16px;
}

.page-description {
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.filter-section {
  margin-bottom: 16px;
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-row {
  margin-bottom: 12px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.record-list-section {
  margin-bottom: 16px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.record-header {
  display: flex;
  margin-bottom: 12px;
}

.record-cover {
  width: 80px;
  height: 60px;
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;
}

.record-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.record-type-tag {
  position: absolute;
  top: 0;
  left: 0;
}

.record-title-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.record-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.record-progress {
  margin-bottom: 12px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  color: #666;
}

.record-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
}

.info-item .anticon {
  margin-right: 6px;
  font-size: 12px;
}

.pagination-section {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.empty-container {
  padding: 24px 0;
}
</style>