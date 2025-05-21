<template>
  <div class="personal-center-page">
    <!-- 顶部导航区域 -->
    <HeaderComponent activeKey="PERSONAL_CENTER"  />

    <div class="page-content">
      <!-- 个人信息区域 -->
      <div class="user-profile-section">
        <div class="user-profile-container">
          <div class="user-avatar">
            <a-avatar :size="92" :src="userProfile.userInfo?.avatar">
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
<!--            <div class="user-department">{{ userProfile.userInfo?.departmentName || '未设置部门' }}</div>-->
<!--            <div class="level-progress">-->
<!--              <div class="progress-bar">-->
<!--                <div class="progress-inner" :style="{ width: levelProgressWidth + '%' }"></div>-->
<!--              </div>-->
<!--              <div class="progress-text">距离下一等级还需 {{ nextLevelCredit }} 学分</div>-->
<!--            </div>-->
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
              <div class="stat-label">学习时长(分钟)</div>
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
<!--          <div class="stat-card">-->
<!--            <div class="stat-icon">-->
<!--              <file-done-outlined />-->
<!--            </div>-->
<!--            <div class="stat-content">-->
<!--              <div class="stat-value">{{ userProfile.statistics?.completedCourseCount || 0 }}</div>-->
<!--              <div class="stat-label">通过考试</div>-->
<!--            </div>-->
<!--          </div>-->
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
        <div class="nav-tabs">
          <div class="tab-item" :class="{ active: activeTab === 'ALL' }" @click="handleTabChange('ALL')">
            全部 ({{ userLearningTotal.allTotal }})
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'TRAIN' }" @click="handleTabChange('TRAIN')">
            培训 ({{ userLearningTotal.trainTotal }})
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'LEARNING_MAP' }" @click="handleTabChange('LEARNING_MAP')">
            地图 ({{ userLearningTotal.mapTotal }})
          </div>
          <!-- <div class="tab-item" :class="{ active: activeTab === 'exam' }" @click="handleTabChange('exam')">
            考试 ({{ userLearningTotal.exam }})
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'practice' }" @click="handleTabChange('practice')">
            练习 ({{ userLearningTotal.practice }})
          </div> -->
        </div>

        <!-- 筛选按钮 -->
        <div class="filter-buttons">
          <a-button
            class="filter-btn"
            :type="filterStatus === 'ALL' ? 'primary' : 'default'"
            shape="round"
            @click="handleFilterChange('ALL')"
          >
            全部
          </a-button>
          <a-button
            class="filter-btn"
            :type="filterStatus === 'ASSIGN' ? 'primary' : 'default'"
            shape="round"
            @click="handleFilterChange('ASSIGN')"
          >
            必修
          </a-button>
          <a-button
            class="filter-btn"
            :type="filterStatus === 'SELF' ? 'primary' : 'default'"
            shape="round"
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
                <span class="record-tag" :class="getTagClass(record.type)">
                  {{ getTypeLabel(record.type) }}
                </span>
              </div>
              <div class="record-info">
                <h3 class="record-title">{{ record.contentName }}</h3>
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

        <!-- 分页 -->
        <div class="pagination-container" v-if="learningRecords.length > 0">
          <a-pagination
            v-model:current="currentPage"
            :total="totalRecords"
            :pageSize="pageSize"
            @change="handlePageChange"
            show-less-items
          />
        </div>
      </div>
    </div>

    <!-- 底部区域 -->
    <footer-component />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getUserProfile, getUserLearningRecords, getUserLearningTotal } from '@/api/toc/personal';
import dayjs from 'dayjs';
import {
  UserOutlined,
  ClockCircleOutlined,
  BookOutlined,
  DeploymentUnitOutlined,
  FileDoneOutlined,
  TrophyOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'PersonalCenterPage',
  components: {
    HeaderComponent,
    FooterComponent,
    EmptyState,
    UserOutlined,
    ClockCircleOutlined,
    BookOutlined,
    DeploymentUnitOutlined,
    FileDoneOutlined,
    TrophyOutlined
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
    const pageSize = ref(12);
    const totalRecords = ref(0);

    // 用户学习统计数据
    const userLearningTotal = ref({});

    // 当前选中的标签和筛选状态
    const activeTab = ref('ALL');
    const filterStatus = ref('ALL');

    // tab数量，从后端获取
    const tabCounts = ref({
      total: 0,
      train: 0,
      map: 0,
      exam: 0,
      practice: 0
    });

    // 计算等级进度
    const levelProgressWidth = computed(() => {
      const level = userProfile.value.userInfo?.level || 1;
      const totalCredit = userProfile.value.statistics?.totalCredit || 0;
      const levelCredit = (level - 1) * 100;
      const nextLevelCredit = level * 100;
      const progress = ((totalCredit - levelCredit) / 100) * 100;
      return Math.min(progress, 100);
    });

    // 计算距离下一等级所需学分
    const nextLevelCredit = computed(() => {
      const level = userProfile.value.userInfo?.level || 1;
      const totalCredit = userProfile.value.statistics?.totalCredit || 0;
      const nextLevelCredit = level * 100;
      return Math.max(nextLevelCredit - totalCredit, 0);
    });

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

    // 获取标签样式类
    const getTagClass = (type) => {
      const classMap = {
        'COURSE': 'tag-course',
        'TRAIN': 'tag-required',
        'LEARNING_MAP': 'tag-elective',
        'EXAM': 'tag-required',
        'PRACTICE': 'tag-elective'
      };
      return classMap[type] || '';
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
      const response = await getUserLearningTotal(true);
      if (response.code === 200) {
        userLearningTotal.value = response.data;
      }
    };

    // 获取学习记录
    const fetchLearningRecords = async () => {
      try {
        const params = {
          type: activeTab.value === 'ALL' ? '' : activeTab.value,
          source: filterStatus.value === 'ALL' ? undefined : filterStatus.value,
          status: 0, // 全部状态
          pageNum: currentPage.value,
          pageSize: pageSize.value
        };

        const response = await getUserLearningRecords(params);
        if (response.code === 200) {
          // 转换数据格式
          learningRecords.value = response.data.list;

          totalRecords.value = response.data.total;

          // 如果是全部类型，更新总数量
          if (activeTab.value === 'ALL') {
            tabCounts.value.total = response.data.total;
          }
        } else {
          message.error(response.message || '获取学习记录失败');
        }
      } catch (error) {
        console.error('获取学习记录异常:', error);
      }
    };

    // 处理标签切换
    const handleTabChange = (tab) => {
      activeTab.value = tab;
      currentPage.value = 1;
      fetchLearningRecords();
      // 注意：tab数量已经从后端获取，不需要在这里更新
    };

    // 处理筛选状态变更
    const handleFilterChange = (status) => {
      filterStatus.value = status;
      currentPage.value = 1;
      fetchLearningRecords();
    };

    // 处理分页变更
    const handlePageChange = (page) => {
      currentPage.value = page;
      fetchLearningRecords();
    };

    // 跳转到详情页
    const navigateToDetail = (record) => {
      const routes = {
        'COURSE': `/toc/course/detail/${record.contentId}`,
        'TRAIN': `/toc/train/detail/${record.contentId}`,
        'LEARNING_MAP': `/toc/map/detail/${record.contentId}`,
        'EXAM': `/toc/exam/detail/${record.contentId}`,
        'PRACTICE': `/toc/practice/detail/${record.contentId}`
      };

      const route = routes[record.type];
      if (route) {
        router.push(route);
      } else {
        message.warning('暂不支持查看该类型的详情');
      }
    };

    // 使用固定的tab数量，不再需要动态计算

    onMounted(() => {
      fetchUserProfile();
      fetchLearningRecords();
      fetchUserLearningTotal();
    });

    return {
      userProfile,
      learningRecords,
      currentPage,
      pageSize,
      totalRecords,
      activeTab,
      filterStatus,
      levelProgressWidth,
      nextLevelCredit,
      tabCounts,
      userLearningTotal,
      getLevelTitle,
      getTypeLabel,
      getTagClass,
      getProgressText,
      handleTabChange,
      handleFilterChange,
      handlePageChange,
      navigateToDetail,
    };
  }
});
</script>

<style scoped>
.personal-center-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  display: flex;
  flex-direction: column;
}

.page-content {
  flex: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}

/* 个人信息区域样式 */
.user-profile-section {
  background-color: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-bottom: 24px;
}

.user-profile-container {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.user-avatar {
  margin-right: 24px;
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
  font-size: 20px;
  font-weight: 500;
  margin: 0;
  margin-right: 12px;
}

.level-tag {
  font-size: 14px;
  color: #1890FF;
  border-radius: 16px;
  padding: 0 12px;
}

.user-department {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 16px;
}

.level-progress {
  width: 100%;
  max-width: 300px;
}

.progress-bar {
  height: 6px;
  background-color: #E5E7EB;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-inner {
  height: 100%;
  background-color: #1890FF;
  border-radius: 3px;
}

.progress-text {
  font-size: 12px;
  color: #9CA3AF;
}

/* 统计卡片样式 */
.statistics-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.stat-card {
  flex: 1;
  min-width: 150px;
  background-color: #FFFFFF;
  border: 1px solid #F3F4F6;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 32px;
  color: #1890FF;
  margin-right: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 18px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
}

/* 学习记录区域样式 */
.learning-records-section {
  background-color: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.nav-tabs {
  display: flex;
  border-bottom: 1px solid #E5E7EB;
  margin-bottom: 16px;
}

.tab-item {
  padding: 16px 24px;
  font-size: 16px;
  color: #4B5563;
  cursor: pointer;
  position: relative;
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
}

.filter-btn {
  border-radius: 9999px;
}

.learning-records-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.learning-record-card {
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  background-color: #FFFFFF;
}

.learning-record-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.record-image {
  height: 191px;
  position: relative;
  background-color: #F3F4F6;
}

.record-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.record-tag {
  position: absolute;
  top: 13px;
  left: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #FFFFFF;
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
  padding: 16px;
}

.record-title {
  font-size: 18px;
  font-weight: 500;
  margin: 0 0 8px 0;
  color: #000000;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-desc {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 16px;
}

.record-progress {
  margin-top: 16px;
}

.record-progress .progress-bar {
  height: 4px;
  margin-bottom: 12px;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-text {
  font-size: 14px;
  color: #6B7280;
}

.credit-text {
  font-size: 14px;
  color: #1890FF;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .user-profile-container {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-avatar {
    margin-right: 0;
    margin-bottom: 16px;
  }

  .level-progress {
    margin: 0 auto;
  }

  .statistics-cards {
    flex-direction: column;
  }

  .learning-records-list {
    grid-template-columns: 1fr;
  }

  .nav-tabs {
    overflow-x: auto;
    white-space: nowrap;
  }

  .tab-item {
    padding: 16px 12px;
  }
}
</style>
