<template>
  <div class="learning-tasks-page">
    <!-- 顶部导航区域 -->
    <HeaderComponent activeKey="PERSONAL_CENTER"  />

    <div class="main-content">
      <!-- 个人信息区域 -->
      <section class="user-info-section">
        <div class="avatar-container">
          <a-avatar :size="96" :src="userInfo.avatar" v-if="userInfo.avatar" />
          <a-avatar :size="96" v-else>{{ userInfo.nickname ? userInfo.nickname.substring(0, 1) : 'U' }}</a-avatar>
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
          <a-tabs v-model:activeKey="activeTaskType" @change="handleTaskTypeChange">
            <a-tab-pane key="TRAIN" :tab="`培训 (${taskCounts.trainTotal || 0})`"></a-tab-pane>
            <a-tab-pane key="LEARNING_MAP" :tab="`地图 (${taskCounts.mapTotal || 0})`"></a-tab-pane>
            <!-- <a-tab-pane key="exam" :tab="`考试 (${taskCounts.exam || 0})`"></a-tab-pane>
            <a-tab-pane key="practice" :tab="`练习 (${taskCounts.practice || 0})`"></a-tab-pane> -->
          </a-tabs>
        </div>

        <!-- 必修/选修筛选 -->
        <!-- <div class="required-filter">
          <a-radio-group v-model:value="requiredFilter" @change="handleRequiredChange" buttonStyle="solid">
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
      </nav>

      <!-- 内容列表区域 -->
      <div class="task-list-section">
        <a-spin :spinning="loading">
          <template v-if="taskList.length > 0">
            <a-row :gutter="[24, 24]">
              <a-col :xs="24" :sm="12" :md="8" v-for="task in taskList" :key="task.id">
                <CourseCard
                  :item="{
                    id: task.id,
                    name: task.title,
                    type: task.type.toUpperCase(),
                    cover: task.coverImage,
                    credit: task.credit,
                    source: task.isRequired === 1 ? 'ASSIGN' : 'SELF',
                    progress: task.progress,
                    deadline: task.endTime,
                    taskCount: task.taskCount
                  }"
                />
              </a-col>
            </a-row>

            <!-- 分页 -->
            <div class="pagination-container">
              <a-pagination
                v-model:current="pagination.current"
                v-model:pageSize="pagination.pageSize"
                :total="pagination.total"
                :showSizeChanger="true"
                :pageSizeOptions="['9', '18', '36', '72']"
                @change="handlePageChange"
                @showSizeChange="handlePageSizeChange"
                show-less-items
              />
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
    </div>

    <!-- 底部区域 -->
    <FooterComponent />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import CourseCard from '@/components/common/CourseCard.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserInfo } from '@/api/toc/user';
import { getLearningTasks } from '@/api/toc/learning';
import { getUserLearningTotal } from '@/api/toc/personal';

export default defineComponent({
  name: 'LearningTasksPage',
  components: {
    HeaderComponent,
    FooterComponent,
    CourseCard,
    EmptyState
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
    const taskCounts = ref({});
    // 加载状态
    const loading = ref(false);

    // 分页信息
    const pagination = reactive({
      current: 1,
      pageSize: 9,
      total: 0
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
    const fetchLearningTasks = async () => {
      loading.value = true;
      try {
        const res = await getLearningTasks({
          type: activeTaskType.value,
          required: requiredFilter.value,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        });

        if (res.code === 200 && res.data) {
          taskList.value = res.data.list || [];
          pagination.total = res.data.total || 0;
        }
      } catch (error) {
        console.error('获取学习任务列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取任务数量统计
    const fetchTaskCounts = async () => {
      try {
        // 获取培训任务数量
        const res = await getUserLearningTotal(false);
        if (res.code === 200 && res.data) {
          taskCounts.value = res.data;
        }

      } catch (error) {
        console.error('获取任务数量统计失败:', error);
      }
    };

    // 处理任务类型变更
    const handleTaskTypeChange = (key) => {
      activeTaskType.value = key;
      pagination.current = 1; // 切换类型时重置页码
      fetchLearningTasks();
    };

    // 处理必修/选修筛选变更
    const handleRequiredChange = () => {
      pagination.current = 1; // 切换筛选条件时重置页码
      fetchLearningTasks();
    };

    // 处理页码变更
    const handlePageChange = (page, pageSize) => {
      pagination.current = page;
      pagination.pageSize = pageSize;
      fetchLearningTasks();
    };

    // 处理每页条数变更
    const handlePageSizeChange = (current, size) => {
      pagination.current = 1; // 切换每页条数时重置页码
      pagination.pageSize = size;
      fetchLearningTasks();
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/learning/center');
    };

    // 获取等级名称
    const getLevelName = (level) => {
      const levelNames = {
        1: '初学者',
        2: '学童',
        3: '秀才',
        4: '举人',
        5: '进士',
        6: '大学士',
        7: '翰林',
        8: '大师',
        9: '宗师',
        10: '大宗师'
      };
      return levelNames[level] || '初学者';
    };

    // 监听筛选条件变化
    watch([activeTaskType, requiredFilter], () => {
      fetchLearningTasks();
    });

    onMounted(() => {
      fetchUserInfo();
      fetchTaskCounts();
      fetchLearningTasks();
    });

    return {
      userInfo,
      activeTaskType,
      requiredFilter,
      taskList,
      taskCounts,
      loading,
      pagination,
      handleTaskTypeChange,
      handleRequiredChange,
      handlePageChange,
      handlePageSizeChange,
      goToLearningCenter,
      getLevelName
    };
  }
});
</script>

<style scoped>
/* 主题颜色变量 - 根据设计稿 */
:root {
  --primary-color: #1890FF; /* 主色调 */
  --primary-bg-light: #EEF2FF; /* 主色调浅色背景 */
  --primary-bg-lighter: #FAF5FF; /* 主色调更浅色背景 */
  --text-primary: #000000; /* 主要文本颜色 */
  --text-secondary: #4B5563; /* 次要文本颜色 */
  --text-tertiary: #6B7280; /* 第三级文本颜色 */
  --border-color: #E5E7EB; /* 边框颜色 */
  --bg-color: #F9FAFB; /* 背景色 */
  --card-bg: #FFFFFF; /* 卡片背景色 */
  --required-tag-bg: #EF4444; /* 必修标签背景色 */
  --elective-tag-bg: #3B82F6; /* 选修标签背景色 */
}

.learning-tasks-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-color);
}

.main-content {
  flex: 1;
  padding: 0 24px;
  max-width: 1392px;
  margin: 0 auto;
  width: 100%;
}

/* 个人信息区域样式 */
.user-info-section {
  display: flex;
  align-items: center;
  background-color: var(--card-bg);
  border-radius: 12px;
  padding: 24px;
  margin-top: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.avatar-container {
  margin-right: 24px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name-level {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.user-name {
  font-size: 20px;
  font-weight: 500;
  margin: 0;
  margin-right: 12px;
}

.level-tag {
  background-color: var(--primary-bg-lighter);
  color: var(--primary-color);
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 14px;
}

.user-credit {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 筛选区域样式 */
.filter-section {
  margin-top: 24px;
}

.task-type-tabs {
  border-bottom: 1px dashed var(--border-color);
}

.required-filter {
  margin-top: 16px;
}

.active-filter {
  background-color: var(--primary-bg-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

/* 内容列表区域样式 */
.task-list-section {
  margin-top: 24px;
  margin-bottom: 48px;
}

.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 自定义 Ant Design 组件样式 */
:deep(.ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: var(--primary-color);
  font-weight: 500;
}

:deep(.ant-tabs-ink-bar) {
  background-color: var(--primary-color);
}

:deep(.ant-radio-button-wrapper:hover) {
  color: var(--primary-color);
}

:deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)) {
  background-color: var(--primary-bg-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .user-info-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-container {
    margin-right: 0;
    margin-bottom: 16px;
  }

  .user-name-level {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-name {
    margin-right: 0;
    margin-bottom: 8px;
  }
}
</style>
