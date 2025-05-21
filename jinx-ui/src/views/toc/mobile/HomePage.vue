
<template>
  <div class="home-mobile-page">
    <!-- 使用移动端头部组件 -->
    <MobileHeader
      title="主页"
      :showBackIcon="false"
      :showLogoSearch="true"
      :showLogo="true"
      :showSearch="true"
      :sticky="true"
      @search="onSearch"
      @search-type-change="onSearchTypeChange"
      @navigate-to-detail="onNavigateToDetail"
    />

    <!-- 搜索结果区域由MobileHeader组件内部处理 -->

    <!-- 主要内容区域 -->
    <div class="main-content" v-show="!showSearchResults">
      <!-- 轮播图 -->
      <div class="carousel-container">
        <a-carousel autoplay>
          <div v-for="banner in banners" :key="banner.id" class="carousel-item" @click="goToBanner(banner)">
            <img :src="banner.imageUrl" :alt="banner.title" />
            <div class="carousel-overlay">
              <h2 class="carousel-title">{{ banner.title }}</h2>
            </div>
          </div>
        </a-carousel>
      </div>

      <!-- 快捷入口 -->
      <div class="section-title">快捷入口</div>
      <div class="quick-access">
        <router-link to="/toc/mobile/learning/tasks" class="quick-access-item">
          <svg-icon icon-class="learning" class="icon-container" />
          <span>学习任务</span>
        </router-link>
        <router-link to="/toc/mobile/learning/center?type=COURSE" class="quick-access-item">
          <svg-icon icon-class="exam" class="icon-container" />
          <span>学习课程</span>
        </router-link>
        <router-link to="/toc/mobile/learning/center?type=TRAIN" class="quick-access-item">
          <svg-icon icon-class="train" class="icon-container" />
          <span>培训列表</span>
        </router-link>
        <router-link to="/toc/mobile/learning/center?type=LEARNING_MAP" class="quick-access-item">
          <svg-icon icon-class="map" class="icon-container" />
          <span>学习地图</span>
        </router-link>
        <router-link to="/toc/mobile/learning/center?type=SURVEY" class="quick-access-item">
          <svg-icon icon-class="survey" class="icon-container" />
          <span>调研任务</span>
        </router-link>
      </div>

      <!-- 学习任务 -->
      <div class="section">
        <div class="section-header">
          <h2>学习任务</h2>
          <div class="task-filters">
            <a-radio-group v-model:value="taskType" button-style="solid" size="small">
              <a-radio-button value="train">培训</a-radio-button>
              <a-radio-button value="map">地图</a-radio-button>
              <!-- <a-radio-button value="exam">考试</a-radio-button> -->
            </a-radio-group>
          </div>
          <router-link to="/toc/mobile/learning/tasks" class="view-all">
            查看全部 <right-outlined />
          </router-link>
        </div>

        <div class="task-list" v-if="filteredTasks.length > 0">
          <div
            v-for="task in filteredTasks.slice(0, 3)"
            :key="task.id"
            class="task-item"
            @click="navigateToDetail(task)"
          >
            <div class="task-cover">
              <img v-if="task.cover" :src="task.cover" :alt="task.name" />
              <div v-else class="cover-placeholder">
                <file-image-outlined />
              </div>
            </div>
            <div class="task-content">
              <h3 class="task-title">{{ task.name }}</h3>
              <div class="task-info">
                <div class="info-item">
                  <clock-circle-outlined />
                  <span>{{ formatDate(task.startTime) }} - {{ formatDate(task.endTime) }}</span>
                </div>
                <div class="info-item" v-if="task.source">
                  <team-outlined />
                  <span>{{ task.source }}</span>
                </div>
              </div>
              <div class="task-progress" v-if="task.progress !== undefined">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${task.progress}%` }"></div>
                </div>
                <span class="progress-text">{{ task.progress }}%</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <empty-outlined />
          <p>暂无学习任务</p>
          <p class="empty-desc">您当前没有需要完成的学习任务</p>
          <a-button type="primary" @click="goToLearningCenter">去学习中心</a-button>
        </div>
      </div>

      <!-- 推荐课程 -->
      <div class="section">
        <div class="section-header">
          <h2>推荐课程</h2>
          <router-link to="/toc/mobile/learning/center" class="view-all">
            查看全部 <right-outlined />
          </router-link>
        </div>

        <div class="course-list" v-if="recommendedCourses.length > 0">
          <div
            v-for="course in recommendedCourses.slice(0, 3)"
            :key="course.id"
            class="course-item"
            @click="navigateToDetail(course)"
          >
            <div class="course-cover">
              <img v-if="course.cover" :src="course.cover" :alt="course.name" />
              <div v-else class="cover-placeholder">
                <file-image-outlined />
              </div>
            </div>
            <div class="course-content">
              <h3 class="course-title">{{ course.name }}</h3>
              <div class="course-info">
                <div class="info-item">
                  <video-camera-outlined />
                  <span>{{ course.lessonCount || 1 }} 门课</span>
                </div>
                <div class="info-item" v-if="course.credit">
                  <trophy-outlined />
                  <span>{{ course.credit }} 学分</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <empty-outlined />
          <p>暂无推荐课程</p>
          <p class="empty-desc">系统暂时没有为您推荐的课程</p>
        </div>
      </div>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="home" />

    <!-- 返回顶部按钮 -->
    <div class="back-to-top" v-show="showBackToTop" @click="scrollToTop">
      <up-outlined />
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  RightOutlined,
  UnorderedListOutlined,
  FileTextOutlined,
  BookOutlined,
  BarChartOutlined,
  OrderedListOutlined,
  TrophyOutlined,
  VideoCameraOutlined,
  FileImageOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  EmptyOutlined,
  UpOutlined
} from '@ant-design/icons-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import MobileHeader from '@/components/mobile/MobileHeader.vue';
import { getHomeData, getBanners } from '@/api/toc/home';
import SvgIcon from '@/components/common/SvgIcon.vue';

export default {
  name: 'HomeMobilePage',
  components: {
    MobileTabBar,
    MobileHeader,
    SvgIcon,
    RightOutlined,
    UnorderedListOutlined,
    FileTextOutlined,
    BookOutlined,
    BarChartOutlined,
    OrderedListOutlined,
    TrophyOutlined,
    VideoCameraOutlined,
    FileImageOutlined,
    ClockCircleOutlined,
    TeamOutlined,
    EmptyOutlined,
    UpOutlined
  },
  setup() {
    const router = useRouter();

    // 轮播图数据
    const banners = ref([]);

    // 搜索相关数据
    const showSearchResults = ref(false);

    // 返回顶部相关
    const showBackToTop = ref(false);

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      departmentName: '',
      totalLearningScore: 0,
      totalStudyDuration: 0,
      totalFinishedTrainCount: 0,
      totalFinishedLearningMapCount: 0,
      acquireCertificateCount: 0
    });

    // 学习任务
    const taskType = ref('train');
    const learningTasks = reactive({
      trainings: [],
      learningMaps: [],
      exams: []
    });

    // 推荐课程
    const recommendedCourses = ref([]);

    // 计算属性：根据选择的任务类型过滤任务
    const filteredTasks = computed(() => {
      switch (taskType.value) {
        case 'train':
          return learningTasks.trainings || [];
        case 'map':
          return learningTasks.learningMaps || [];
        case 'exam':
          return learningTasks.exams || [];
        default:
          return [];
      }
    });

    // 格式化学习时长
    const formatStudyTime = (minutes) => {
      if (!minutes) return '0 分钟';
      return `${minutes} 分钟`;
    };

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/mobile/learning/center');
    };

    // 处理MobileHeader组件的搜索事件
    const onSearch = (params) => {
      showSearchResults.value = true;
    };

    // 处理MobileHeader组件的搜索类型变化事件
    const onSearchTypeChange = (type) => {
      // 可以在这里处理搜索类型变化的逻辑
    };

    // 处理MobileHeader组件的导航到详情页事件
    const onNavigateToDetail = (item) => {
      showSearchResults.value = false;
    };

    // 导航到详情页面
    const navigateToDetail = (item) => {
      if (!item || !item.id) {
        message.error('无效的项目数据');
        return;
      }

      // 根据项目类型导航到不同的详情页
      if (item.type === 'COURSE' ) {
        // 课程或培训详情页
        router.push(`/toc/mobile/course/${item.id}`);
      } else if (item.type === 'TRAIN') {
        // 培训情页
        router.push(`/toc/mobile/train/${item.id}`);
      } else if (item.type === 'LEARNING_MAP') {
        // 学习地图详情页
        router.push(`/toc/mobile/map/${item.id}`);
      } else if (item.type === 'EXAM' || taskType.value === 'exam') {
        // 考试详情页
        router.push(`/toc/mobile/learning/center?type=EXAM&id=${item.id}`);
      } else {
        // 默认导航到课程详情页
        router.push(`/toc/mobile/course/${item.id}`);
      }
    };

    // 滚动到顶部
    const scrollToTop = () => {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    };

    // 监听页面滚动
    const handleScroll = () => {
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      // 当滚动超过一定距离时，显示返回顶部按钮
      if (scrollTop > 100) {
        showBackToTop.value = true;
      } else {
        showBackToTop.value = false;
      }
    };

    // 获取首页数据
    const fetchHomeData = async () => {
      try {
        const { data } = await getHomeData();
        if (data) {
          // 设置用户信息
          userInfo.value = data.userInfo || {};

          // 设置学习任务
          if (data.learningTasks) {
            learningTasks.trainings = data.learningTasks.trainings || [];
            learningTasks.learningMaps = data.learningTasks.learningMaps || [];
            learningTasks.exams = data.learningTasks.exams || [];
          }

          // 设置推荐课程
          if (data.recommendedCourses) {
            // 合并所有类型的推荐内容
            recommendedCourses.value = [
              ...(data.recommendedCourses.courses || []),
              ...(data.recommendedCourses.trainings || []),
              ...(data.recommendedCourses.learningMaps || [])
            ];
          }
        }
      } catch (error) {
        console.error('获取首页数据失败:', error);
      }
    };

    const goToBanner = (banner) => {
      window.open(banner.linkUrl, '_blank');
    };

    // 获取轮播图数据
    const fetchBanners = async () => {
      try {
        const { data } = await getBanners({
          type: 'MOBILE'
        });
        banners.value = data || [];
      } catch (error) {
        console.error('获取轮播图数据失败:', error);
      }
    };

    // 组件挂载时获取数据和添加滚动监听
    onMounted(() => {
      fetchHomeData();
      fetchBanners();

      // 添加滚动监听
      window.addEventListener('scroll', handleScroll);
    });

    // 组件卸载时移除滚动监听
    const onBeforeUnmount = () => {
      window.removeEventListener('scroll', handleScroll);
    };

    return {
      banners,
      userInfo,
      taskType,
      learningTasks,
      filteredTasks,
      recommendedCourses,
      formatStudyTime,
      formatDate,
      goToLearningCenter,
      showSearchResults,
      // MobileHeader组件事件处理函数
      onSearch,
      onSearchTypeChange,
      onNavigateToDetail,
      // 返回顶部相关
      showBackToTop,
      scrollToTop,
      onBeforeUnmount,
      // 导航到详情页函数
      navigateToDetail,
      goToBanner
    };
  }
};
</script>

<style scoped>
.home-mobile-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  padding-bottom: 80px; /* 为底部导航栏留出空间 */
}







/* 主要内容区域 */
.main-content {
  padding: 16px;
}

/* 轮播图 */
.carousel-container {
  height: 160px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}

.carousel-item {
  height: 160px;
  position: relative;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
}

.carousel-title {
  color: #FFFFFF;
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

/* 快捷入口 */
.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #1F2937;
}

.quick-access {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 24px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.quick-access-item {
  background-color: #FFFFFF;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  text-decoration: none;
  color: #374151;
  box-shadow: 0px 1px 3px rgba(0, 0, 0, 0.1);
}

.icon-container {
  width: 40px;
  height: 40px;
  background-color: rgba(79, 70, 229, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  font-size: 18px;
  color: #1890FF;
}

.quick-access-item span {
  font-size: 12px;
  text-align: center;
}

/* 通用区块样式 */
.section {
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0px 1px 3px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.section-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.task-filters {
  margin: 8px 0;
  order: 3;
  width: 100%;
}

.view-all {
  color: #1890FF;
  text-decoration: none;
  font-size: 14px;
  display: flex;
  align-items: center;
}

/* 学习任务和课程列表 */
.task-item, .course-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #E5E7EB;
}

.task-item:last-child, .course-item:last-child {
  border-bottom: none;
}

.task-cover, .course-cover {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  background-color: #E5E7EB;
  margin-right: 12px;
  flex-shrink: 0;
}

.task-cover img, .course-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
  font-size: 24px;
}

.task-content, .course-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.task-title, .course-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-info, .course-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #6B7280;
}

.info-item .anticon {
  margin-right: 4px;
  font-size: 12px;
}

.task-progress {
  display: flex;
  align-items: center;
  margin-top: auto;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background-color: #E5E7EB;
  border-radius: 2px;
  overflow: hidden;
  margin-right: 8px;
}

.progress-fill {
  height: 100%;
  background-color: #1890FF;
  border-radius: 2px;
}

.progress-text {
  font-size: 12px;
  color: #1890FF;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  color: #9CA3AF;
}

.empty-state .anticon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.empty-state .empty-desc {
  font-size: 12px;
  margin: 4px 0 16px;
}

/* 响应式调整 */
@media (max-width: 360px) {
  .quick-access {
    grid-template-columns: repeat(3, 1fr);
  }
}



/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 80px; /* 底部导航栏上方 */
  right: 16px;
  width: 40px;
  height: 40px;
  background-color: #1890FF;
  color: #FFFFFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 90;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-to-top:active {
  transform: scale(0.95);
}
</style>
