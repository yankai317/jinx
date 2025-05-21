<template>
  <div class="personal-center-mobile-page">
    <!-- 顶部个人信息区域 -->
    <div class="user-profile-section">
      <div class="user-profile-container">
        <div class="user-avatar">
          <a-avatar :size="64" :src="userProfile.userInfo?.avatar">
            <template #icon><user-outlined /></template>
          </a-avatar>
        </div>
        <div class="user-info">
          <div class="user-name-level">
            <h2 class="user-name">{{ userProfile.userInfo?.nickname || '用户' }}</h2>
            <a-tag class="level-tag" color="#EEF2FF">
              LV{{ userProfile.userInfo?.level || 1 }} {{ getLevelTitle(userProfile.userInfo?.level) }}
            </a-tag>
          </div>
        </div>
      </div>

      <!-- 快捷入口区域 -->
      <div class="quick-links">
        <div class="quick-link-item" @click="goToLearningTasks">
          <div class="quick-link-icon">
            <schedule-outlined />
          </div>
          <div class="quick-link-text">我的学习任务</div>
          <right-outlined class="quick-link-arrow" />
        </div>
      </div>

      <!-- 学习统计数据区域 -->
      <div class="statistics-cards">
        <div class="stat-card">
          <div class="stat-icon">
            <clock-circle-outlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProfile.statistics?.totalStudyTime || 0 }}</div>
            <div class="stat-label">学习时长<br/>(分钟)</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <book-outlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProfile.statistics?.completedTrainCount || 0 }}</div>
            <div class="stat-label">完成培训</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <deployment-unit-outlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProfile.statistics?.completedMapCount || 0 }}</div>
            <div class="stat-label">完成地图</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <trophy-outlined />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProfile.statistics?.certificateCount || 0 }}</div>
            <div class="stat-label">获得证书</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 学习记录区域 -->
    <div class="learning-records-section">
      <!-- 导航标签 -->
      <!-- <div class="nav-tabs">
        <div class="tab-item" :class="{ active: activeTab === 'ALL' }" @click="handleTabChange('ALL')">
          全部 ({{ userLearningTotal.allTotal || 0 }})
        </div>
        <div class="tab-item" :class="{ active: activeTab === 'COURSE' }" @click="handleTabChange('COURSE')">
          课程 ({{ userLearningTotal.courseTotal || 0 }})
        </div>
        <div class="tab-item" :class="{ active: activeTab === 'TRAIN' }" @click="handleTabChange('TRAIN')">
          培训 ({{ userLearningTotal.trainTotal || 0 }})
        </div>
        <div class="tab-item" :class="{ active: activeTab === 'LEARNING_MAP' }" @click="handleTabChange('LEARNING_MAP')">
          地图 ({{ userLearningTotal.mapTotal || 0 }})
        </div>
      </div> -->
      <div class="tabs-title">
        <div class="title">学习记录</div>
        <div class="view-all" @click="goToLearningTasks">查看全部</div>
      </div>

      <!-- 筛选按钮 -->
      <div class="filter-buttons">
        <a-button
          class="filter-btn"
          :type="filterStatus === 'ALL' ? 'primary' : 'default'"
          shape="round"
          size="small"
          @click="handleFilterChange('ALL')"
        >
          全部
        </a-button>
        <a-button
          class="filter-btn"
          :type="filterStatus === 'ASSIGN' ? 'primary' : 'default'"
          shape="round"
          size="small"
          @click="handleFilterChange('ASSIGN')"
        >
          必修
        </a-button>
        <a-button
          class="filter-btn"
          :type="filterStatus === 'SELF' ? 'primary' : 'default'"
          shape="round"
          size="small"
          @click="handleFilterChange('SELF')"
        >
          选修
        </a-button>
      </div>

      <!-- 学习记录列表 -->
      <div class="learning-records-list" v-if="learningRecords.length > 0">
        <div v-for="record in learningRecords" :key="record.id" class="learning-record-card" @click="navigateToDetail(record)">
          <div class="record-image">
            <img :src="record.coverUrl || 'https://via.placeholder.com/339x191'" alt="封面图" />
            <a-tag :color="getTypeColor(record.type)">{{ getTypeLabel(record.type) }}</a-tag>
          </div>
          <div class="record-info">
            <h3 class="record-title">{{ record.title }}</h3>
            <p class="record-desc">{{ record.description }}</p>
            <div class="record-progress">
              <div class="progress-bar">
                <div class="progress-inner" :style="{ width: record.progress + '%' }"></div>
              </div>
              <div class="progress-details">
                <span class="progress-text">{{ getProgressText(record) }}</span>
                <span class="credit-text">{{ record.credit }} 学分</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <empty-state v-else title="暂无学习记录" description="您还没有任何学习记录" />

      <!-- 加载更多提示 -->
      <div class="loading-more" v-if="learningRecords.length > 0 && hasMoreRecords && loading">
        <a-spin size="small" />
        <span>加载中...</span>
      </div>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="profile" />

    <!-- 底部占位，防止内容被底部导航栏遮挡 -->
    <div class="bottom-placeholder"></div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, reactive, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserProfile, getUserLearningRecords, getUserLearningTotal } from '@/api/toc/personal';
import {
  UserOutlined,
  ClockCircleOutlined,
  BookOutlined,
  DeploymentUnitOutlined,
  TrophyOutlined,
  ScheduleOutlined,
  RightOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'PersonalCenterMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    UserOutlined,
    ClockCircleOutlined,
    BookOutlined,
    DeploymentUnitOutlined,
    TrophyOutlined,
    ScheduleOutlined,
    RightOutlined
  },
  setup() {
    const router = useRouter();

    // 用户个人中心数据
    const userProfile = ref({
      userInfo: {},
      statistics: {},
      certificates: [],
      recentLearning: []
    });

    // 学习记录
    const learningRecords = ref([]);
    const currentPage = ref(1);
    const pageSize = ref(2); // 移动端每页显示较少的记录，但增加数量以便触底加载
    const totalRecords = ref(0);
    const loading = ref(false);
    const hasMoreRecords = computed(() => {
      return learningRecords.value.length < totalRecords.value;
    });

    // 用户学习统计数据
    const userLearningTotal = ref({
      allTotal: 0,
      courseTotal: 0,
      trainTotal: 0,
      mapTotal: 0
    });

    // 当前选中的标签和筛选状态
    const activeTab = ref('ALL');
    const filterStatus = ref('ALL');

    // 获取等级称号
    const getLevelTitle = (level) => {
      const titles = [''];
      return titles[level] || '';
    };

    // 获取内容类型标签
    const getTypeLabel = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'TRAIN': '培训',
        'LEARNING_MAP': '地图',
        'EXAM': '考试',
        'PRACTICE': '练习'
      };
      return typeMap[type] || '未知类型';
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


    // 获取进度文本
    const getProgressText = (record) => {
      if (record.status === 1) {
        return '已完成';
      } else if (record.progress > 0) {
        return `进度 ${record.progress}%`;
      } else {
        return '未开始';
      }
    };

    // 获取用户个人中心数据
    const fetchUserProfile = async () => {
      try {
        const response = await getUserProfile();
        if (response.code === 200) {
          userProfile.value = response.data;
        } else {
          message.error(response.message || '获取个人中心数据失败');
        }
      } catch (error) {
        console.error('获取个人中心数据异常:', error);
      }
    };

    // 获取用户学习统计数据
    const fetchUserLearningTotal = async () => {
      try {
        const response = await getUserLearningTotal(true);
        if (response.code === 200) {
          userLearningTotal.value = response.data;
        } else {
          message.error(response.message || '获取学习统计数据失败');
        }
      } catch (error) {
        console.error('获取学习统计数据异常:', error);
      }
    };

    // 获取学习记录
    const fetchLearningRecords = async (isLoadMore = false) => {
      try {
        loading.value = true;
        const params = {
          type: activeTab.value === 'ALL' ? '' : activeTab.value,
          source: filterStatus.value === 'ALL' ? undefined : filterStatus.value,
          status: 0, // 全部状态
          pageNum: isLoadMore ? currentPage.value : 1,
          pageSize: pageSize.value
        };

        const response = await getUserLearningRecords(params);
        if (response.code === 200) {
          if (isLoadMore) {
            //，追加数据
            learningRecords.value = [...learningRecords.value, ...response.data.list];
          } else {
            // 切换标签时，替换数据
            learningRecords.value = response.data.list;
            currentPage.value = 1;
          }
          totalRecords.value = response.data.total;
        } else {
          message.error(response.message || '获取学习记录失败');
        }
      } catch (error) {
        console.error('获取学习记录异常:', error);
      } finally {
        loading.value = false;
      }
    };

    // 监听页面滚动，实现触底加载更多
    const handleScroll = () => {
      // 如果正在加载更多，或者没有更多数据了，则不执行
      if (loading.value || !hasMoreRecords.value) return;

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

    // 处理标签切换
    const handleTabChange = (tab) => {
      if (activeTab.value === tab) return;
      activeTab.value = tab;
      fetchLearningRecords();
    };

    // 处理筛选状态变更
    const handleFilterChange = (status) => {
      if (filterStatus.value === status) return;
      filterStatus.value = status;
      fetchLearningRecords();
    };

    // 加载更多
    const loadMore = () => {
      if (loading.value || !hasMoreRecords.value) return;
      loading.value = true;
      currentPage.value += 1;
      fetchLearningRecords(true);
    };

    // 跳转到详情页
    const navigateToDetail = (record) => {
      const routes = {
        'COURSE': `/toc/mobile/course/${record.contentId}`,
        'TRAIN': `/toc/mobile/train/${record.contentId}`,
        'LEARNING_MAP': `/toc/mobile/map/${record.contentId}`,
        'EXAM': `/toc/mobile/exam/${record.contentId}`,
        'PRACTICE': `/toc/mobile/practice/${record.contentId}`
      };

      const route = routes[record.type];
      if (route) {
        router.push(route);
      } else {
        message.warning('暂不支持查看该类型的详情');
      }
    };

    onMounted(() => {
      fetchUserProfile();
      fetchLearningRecords();
      fetchUserLearningTotal();

      // 添加滚动事件监听
      window.addEventListener('scroll', handleScroll);
    });

    // 组件卸载时，移除滚动事件监听
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll);
    });


    // 跳转到学习任务页面
    const goToLearningTasks = () => {
      router.push('/toc/mobile/learning/tasks');
    };

    return {
      userProfile,
      learningRecords,
      loading,
      hasMoreRecords,
      activeTab,
      filterStatus,
      userLearningTotal,
      getLevelTitle,
      getTypeLabel,
      getTypeColor,
      getProgressText,
      handleTabChange,
      handleFilterChange,
      loadMore,
      navigateToDetail,
      goToLearningTasks
    };
  }
});
</script>

<style scoped>
.personal-center-mobile-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  padding-bottom: 65px; /* 为底部导航栏留出空间 */
}

/* 个人信息区域样式 */
.user-profile-section {
  background-color: #FFFFFF;
  padding: 16px;
  margin-bottom: 12px;
}

.user-profile-container {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.user-avatar {
  margin-right: 16px;
}

.user-info {
  flex: 1;
}

.user-name-level {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.user-name {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
  margin-right: 8px;
}

.level-tag {
  font-size: 12px;
  color: #1890FF;
  border-radius: 16px;
  padding: 0 8px;
  margin-top: 4px;
}

/* 快捷入口区域样式 */
.quick-links {
  margin: 16px 0;
  background-color: #FFFFFF;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #F3F4F6;
}

.quick-link-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #F3F4F6;
  cursor: pointer;
}

.quick-link-item:last-child {
  border-bottom: none;
}

.quick-link-icon {
  font-size: 20px;
  color: #1890FF;
  margin-right: 12px;
}

.quick-link-text {
  flex: 1;
  font-size: 15px;
  color: #111827;
}

.quick-link-arrow {
  color: #9CA3AF;
  font-size: 14px;
}

/* 统计卡片样式 */
.statistics-cards {
  display: flex;
  gap: 12px;
}

.stat-card {
  background-color: #FFFFFF;
  border: 1px solid #F3F4F6;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1;
}

.stat-icon {
  font-size: 24px;
  color: #1890FF;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 2px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
  text-align: center;
}

/* 学习记录区域样式 */
.learning-records-section {
  background-color: #FFFFFF;
  padding: 16px;
}

.tabs-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
}

.view-all {
  font-size: 14px;
  color: #1890FF;
  cursor: pointer;
}



.nav-tabs {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid #E5E7EB;
  margin-bottom: 12px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none; /* Firefox */
}

.nav-tabs::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Edge */
}

.tab-item {
  padding: 12px 16px;
  font-size: 14px;
  color: #4B5563;
  cursor: pointer;
  position: relative;
  flex-shrink: 0;
}

.tab-item.active {
  color: #1890FF;
  font-weight: 700;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #1890FF;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.filter-buttons::-webkit-scrollbar {
  display: none;
}

.filter-btn {
  border-radius: 9999px;
  flex-shrink: 0;
}

.learning-records-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.learning-record-card {
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  overflow: hidden;
  background-color: #FFFFFF;
}

.record-image {
  height: 160px;
  position: relative;
  background-color: #F3F4F6;
}

.record-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.record-image .ant-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.tag-required {
  background-color: #EF4444;
}

.tag-elective {
  background-color: #3B82F6;
}

.tag-course {
  background-color: #10B981;
}

.record-info {
  padding: 12px;
}

.record-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 6px 0;
  color: #000000;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-desc {
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 36px;
}

.record-progress .progress-bar {
  height: 4px;
  background-color: #E5E7EB;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-inner {
  height: 100%;
  background-color: #1890FF;
  border-radius: 2px;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-text {
  font-size: 12px;
  color: #6B7280;
}

.credit-text {
  font-size: 12px;
  color: #1890FF;
}

/* 加载更多提示 */
.loading-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0;
  gap: 8px;
  color: #9CA3AF;
  font-size: 14px;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  margin-bottom: 10px;
}

.bottom-placeholder {
  height: 65px; /* 与底部导航栏高度一致 */
}
</style>
