<template>
  <div class="learning-tasks-mobile-page">
    <div class="main-content">
      <!-- 个人信息区域 -->
      <section class="user-info-section">
        <div class="avatar-container">
          <a-avatar :size="64" :src="userInfo.avatar" v-if="userInfo.avatar" />
          <a-avatar :size="64" v-else>{{ userInfo.nickname ? userInfo.nickname.substring(0, 1) : 'U' }}</a-avatar>
        </div>
        <div class="user-info">
          <div class="user-name-level">
            <h2 class="user-name">{{ userInfo.nickname || '用户' }}</h2>
            <span class="level-tag">LV{{ userInfo.level || 1 }} {{ getLevelName(userInfo.level) }}</span>
          </div>
          <div class="user-credit">
            <span>学分：{{ userInfo.totalCredit || 0 }}</span>
          </div>
        </div>
      </section>

      <!-- 筛选区域 -->
      <nav class="filter-section">
        <!-- 任务类型筛选 -->
        <div class="task-type-tabs">
          <a-tabs v-model:activeKey="activeTaskType" @change="handleTaskTypeChange" size="small">
            <a-tab-pane key="TRAIN" :tab="`培训 (${taskCounts.trainTotal || 0})`"></a-tab-pane>
            <a-tab-pane key="LEARNING_MAP" :tab="`地图 (${taskCounts.mapTotal || 0})`"></a-tab-pane>
<!--            <a-tab-pane key="exam" :tab="`考试 (${taskCounts.exam || 0})`"></a-tab-pane>-->
<!--            <a-tab-pane key="practice" :tab="`练习 (${taskCounts.practice || 0})`"></a-tab-pane>-->
          </a-tabs>
        </div>

        <!-- 必修/选修筛选 -->
        <!-- <div class="required-filter">
          <a-radio-group v-model:value="requiredFilter" @change="handleRequiredChange" buttonStyle="solid" size="small">
            <a-radio-button
              :value="0"
              :class="{ 'active-filter': requiredFilter === 0 }"
            >全部</a-radio-button>
            <a-radio-button
              :value="1"
              :class="{ 'active-filter': requiredFilter === 1 }"
            >必修</a-radio-button>
            <a-radio-button
              :value="2"
              :class="{ 'active-filter': requiredFilter === 2 }"
            >选修</a-radio-button>
          </a-radio-group>
        </div> -->
        <!-- 内容列表区域 -->
        <div class="task-list-section">
          <a-spin :spinning="loading">
            <template v-if="taskList.length > 0">
              <div class="task-list">
                <div
                  v-for="task in taskList"
                  :key="task.id"
                  class="task-item"
                  @click="navigateToDetail(task)"
                >
                  <div class="task-cover">
                    <img v-if="task.coverImage" :src="task.coverImage" :alt="task.title" />
                    <div v-else class="cover-placeholder">
                      <file-image-outlined />
                    </div>
                    <div class="task-type-tag">
                      <a-tag :color="getTypeColor(task.type)">{{ getTypeText(task.type) }}</a-tag>
                    </div>
                    <div class="task-source-tag" v-if="task.isRequired === 1">
                      <a-tag color="red">必修</a-tag>
                    </div>
                  </div>
                  <div class="task-content">
                    <h3 class="task-title">{{ task.title }}</h3>
                    <div class="task-info">
                      <div class="info-item" v-if="task.credit">
                        <trophy-outlined />
                        <span>{{ task.credit }} 学分</span>
                      </div>
                      <div class="info-item" v-if="task.endTime">
                        <clock-circle-outlined />
                        <span>截止: {{ formatDate(task.endTime) }}</span>
                      </div>
                      <div class="info-item" v-if="task.taskCount">
                        <ordered-list-outlined />
                        <span>{{ task.taskCount }} 个子任务</span>
                      </div>
                    </div>
                    <div class="task-progress">
                      <div class="progress-bar">
                        <div
                          class="progress-fill"
                          :style="{ width: `${task.progress || 0}%` }"
                          :class="{ 'completed': task.progress === 100 }"
                        ></div>
                      </div>
                      <span class="progress-text">{{ task.progress || 0 }}%</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="hasMore && loadingMore" class="loading-more">
                <a-spin size="small" />
                <span>加载中...</span>
              </div>
            </template>
            <!-- 空状态展示 -->
            <EmptyState
              v-else
              title="暂无学习任务"
              description="当前没有符合条件的学习任务，请尝试更换筛选条件"
            >
              <template #action>
                <a-button type="primary" @click="goToLearningCenter">
                  去学习中心
                </a-button>
              </template>
            </EmptyState>
          </a-spin>
        </div>
      </nav>

    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="profile" />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserInfo } from '@/api/toc/user';
import { getLearningTasks } from '@/api/toc/learning';
import {
  FileImageOutlined,
  TrophyOutlined,
  ClockCircleOutlined,
  OrderedListOutlined
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { message } from 'ant-design-vue';
import { getUserLearningTotal } from '@/api/toc/personal';

export default defineComponent({
  name: 'LearningTasksMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    FileImageOutlined,
    TrophyOutlined,
    ClockCircleOutlined,
    OrderedListOutlined
  },
  setup() {
    const router = useRouter();
    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      level: 1,
      totalCredit: 0,
      departmentName: '',
      completedTrainCount: 0,
      completedMapCount: 0,
      certificateCount: 0
    });

    // 任务类型筛选
    const activeTaskType = ref('TRAIN');
    // 必修/选修筛选
    const requiredFilter = ref(0);
    // 任务列表
    const taskList = ref([]);
    // 任务数量统计
    const taskCounts = ref({
      allTotal: 0,
      courseTotal: 0,
      mapTotal: 0,
      trainTotal: 0
    });
    // 加载状态
    const loading = ref(false);
    const loadingMore = ref(false);

    // 分页信息
    const pagination = reactive({
      current: 1,
      pageSize: 5,
      total: 0
    });

    // 计算属性：是否有更多数据
    const hasMore = computed(() => {
      return taskList.value.length < pagination.total;
    });

    // 获取用户信息
    const fetchUserInfo = async () => {
      try {
        loading.value = true;
        const res = await getUserInfo();
        if (res.code === 200 && res.data) {
          userInfo.value = res.data;
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取学习任务列表
    const fetchLearningTasks = async (isLoadMore = false) => {
      if (!isLoadMore) {
        loading.value = true;
      } else {
        loadingMore.value = true;
      }

      try {
        const res = await getLearningTasks({
          type: activeTaskType.value,
          required: requiredFilter.value,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        });

        if (res.code === 200 && res.data) {
          if (isLoadMore) {
            taskList.value = [...taskList.value, ...(res.data.list || [])];
          } else {
            taskList.value = res.data.list || [];
          }
          pagination.total = res.data.total || 0;
        }
      } catch (error) {
        console.error('获取学习任务列表失败:', error);
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    };

    // 获取用户学习统计数据
    const fetchUserLearningTotal = async () => {
      try {
        const response = await getUserLearningTotal(false);
        if (response.code === 200) {
          taskCounts.value = response.data;
        } else {
          message.error(response.message || '获取学习统计数据失败');
        }
      } catch (error) {
        console.error('获取学习统计数据异常:', error);
      }
    };

    // 处理任务类型变更
    const handleTaskTypeChange = () => {
      pagination.current = 1;
      fetchLearningTasks();
    };

    // 处理必修/选修筛选变更
    const handleRequiredChange = () => {
      pagination.current = 1;
      fetchLearningTasks();
    };

    // 监听页面滚动，实现触底加载更多
    const handleScroll = () => {
      // 如果正在加载更多，或者没有更多数据了，则不执行
      if (loadingMore.value || !hasMore.value) return;

      // 获取页面滚动位置
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      // 获取页面可见区域高度
      const windowHeight = window.innerHeight;
      // 获取页面总高度
      const documentHeight = document.documentElement.scrollHeight;

      // 检测是否滚动到底部(距离底部100px时就开始加载)
      if (scrollTop + windowHeight >= documentHeight - 100) {
        loadMore();
      }
    };

    // 加载更多
    const loadMore = () => {
      if (loadingMore.value || !hasMore.value) return;
      loadingMore.value = true;
      pagination.current += 1;
      fetchLearningTasks(true);
    };

    // 处理页码变更
    const handlePageChange = (page) => {
      pagination.current = page;
      fetchLearningTasks();
    };

    // 获取等级名称
    const getLevelName = (level) => {
      const levelMap = {
        1: '初学者',
        2: '学习新手',
        3: '知识探索者',
        4: '进阶学习者',
        5: '知识达人',
        6: '学习专家',
        7: '知识大师',
        8: '学习导师',
        9: '知识权威',
        10: '终身学习者'
      };
      return levelMap[level] || '学习者';
    };

    // 获取任务类型文本
    const getTypeText = (type) => {
      const typeMap = {
        'TRAIN': '培训',
        'LEARNING_MAP': '学习地图',
        'EXAM': '考试',
        'PRACTICE': '练习'
      };
      return typeMap[type] || type;
    };

    // 获取任务类型颜色
    const getTypeColor = (type) => {
      const colorMap = {
        'TRAIN': 'blue',
        'LEARNING_MAP': 'purple',
        'EXAM': 'orange',
        'PRACTICE': 'green'
      };
      return colorMap[type] || 'default';
    };

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      return dayjs(dateStr).format('MM-DD');
    };

    // 导航到详情页
    const navigateToDetail = (task) => {
      const typeRouteMap = {
        'TRAIN': `/toc/mobile/train/${task.id}`,
        'LEARNING_MAP': `/toc/mobile/map/${task.id}`,
        'EXAM': `/toc/mobile/exam/${task.id}`,
        'PRACTICE': `/toc/mobile/practice/${task.id}`
      };

      const route = typeRouteMap[task.type];
      if (route) {
        router.push(route);
      }
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/mobile/learning/center');
    };

    onMounted(() => {
      fetchUserInfo();
      fetchLearningTasks();
      fetchUserLearningTotal();

      // 添加滚动事件监听
      window.addEventListener('scroll', handleScroll);
    });

    // 组件卸载时，移除滚动事件监听
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll);
    });

    return {
      userInfo,
      activeTaskType,
      requiredFilter,
      taskList,
      taskCounts,
      loading,
      loadingMore,
      pagination,
      hasMore,
      getLevelName,
      getTypeText,
      getTypeColor,
      formatDate,
      handleTaskTypeChange,
      handleRequiredChange,
      handlePageChange,
      navigateToDetail,
      goToLearningCenter
    };
  }
});
</script>

<style scoped>
.learning-tasks-mobile-page {
  padding-bottom: 65px;
}

.main-content {
  padding: 16px;
}

.user-info-section {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.avatar-container {
  margin-right: 16px;
}

.user-info {
  flex: 1;
}

.user-name-level {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.user-name {
  font-size: 18px;
  margin: 0;
  margin-right: 8px;
}

.level-tag {
  font-size: 12px;
  color: #fff;
  background-color: #1890ff;
  padding: 2px 6px;
  border-radius: 10px;
}

.user-credit {
  font-size: 14px;
  color: #666;
}

.filter-section {
  margin-bottom: 16px;
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-type-tabs {
  margin-bottom: 12px;
}

.required-filter {
  display: flex;
  justify-content: center;
}

.active-filter {
  color: #1890ff;
  border-color: #1890ff;
}

.task-list-section {
  margin-bottom: 16px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-cover {
  position: relative;
  width: 100%;
  height: 140px;
}

.task-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  color: #999;
}

.cover-placeholder .anticon {
  font-size: 36px;
}

.task-type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.task-source-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.task-content {
  padding: 12px;
}

.task-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
  margin-right: 12px;
}

.info-item .anticon {
  margin-right: 4px;
  font-size: 12px;
}

.task-progress {
  display: flex;
  align-items: center;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  margin-right: 8px;
}

.progress-fill {
  height: 100%;
  background-color: #1890ff;
  border-radius: 3px;
}

.progress-fill.completed {
  background-color: #52c41a;
}

.progress-text {
  font-size: 12px;
  color: #666;
  min-width: 36px;
  text-align: right;
}

/* 加载更多 */
.loading-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0;
  gap: 8px;
  color: #9CA3AF;
  font-size: 14px;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
