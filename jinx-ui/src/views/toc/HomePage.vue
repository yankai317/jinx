<template>
  <div class="home-page">
    <!-- 顶部导航栏11 -->
    <HeaderComponent activeKey="HOME" />

    <div class="main-content">
      <!-- 轮播图和个人信息 -->
      <div class="top-section">
        <div class="carousel-container">
          <a-carousel autoplay arrows>
            <template #prevArrow>
              <div class="custom-slick-arrow" style="left: 10px; z-index: 1">
                <LeftOutlined style="font-size: 36px; color: #fff;" />
              </div>
            </template>
            <template #nextArrow>
              <div class="custom-slick-arrow" style="right: 24px">
                <RightOutlined style="font-size: 36px; color: #fff;" />
              </div>
            </template>
            <div v-for="banner in banners" :key="banner.id" class="carousel-item" @click="goToBanner(banner)">
              <img :src="banner.imageUrl" :alt="banner.title" />
              <div class="carousel-overlay">
                <h1 class="carousel-title">{{ banner.title }}</h1>
              </div>
            </div>
          </a-carousel>
        </div>

        <div class="user-info-card">
          <div class="user-profile">
            <a-avatar :size="64" :src="userInfo.avatar" />
            <div class="user-details">
              <h3>{{ userInfo.nickname }}</h3>
              <p>{{ userInfo.departmentName }}</p>
            </div>
          </div>

          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-value">{{ userInfo.totalLearningScore || 0 }}</div>
              <div class="stat-label">总学分</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ formatStudyTime(userInfo.totalStudyDuration) }}</div>
              <div class="stat-label">学时（分）</div>
            </div>
          </div>

          <div class="user-achievements">
            <div class="achievement-item">
              <span class="achievement-label">完成培训</span>
              <span class="achievement-value">{{ userInfo.totalFinishedTrainCount || 0 }} 个</span>
            </div>
            <div class="achievement-item">
              <span class="achievement-label">完成学习地图</span>
              <span class="achievement-value">{{ userInfo.totalFinishedLearningMapCount || 0 }} 个</span>
            </div>
            <!-- <div class="achievement-item">
              <span class="achievement-label">通过考试</span>
              <span class="achievement-value">{{ userInfo.passedExamCount || 0 }} 次</span>
            </div> -->
            <div class="achievement-item">
              <span class="achievement-label">获得证书</span>
              <span class="achievement-value">{{ userInfo.acquireCertificateCount || 0 }} 个</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-access">
        <router-link to="/toc/learning/tasks" class="quick-access-item">
          <svg-icon icon-class="learning" class="icon-container" />
          <span>学习任务</span>
        </router-link>
        <router-link to="/toc/learning/center?type=COURSE" class="quick-access-item">
          <svg-icon icon-class="exam" class="icon-container" />
          <span>学习课程</span>
        </router-link>
        <router-link to="/toc/learning/center?type=TRAIN" class="quick-access-item">
          <svg-icon icon-class="train" class="icon-container" />
          <span>培训列表</span>
        </router-link>
        <router-link to="/toc/learning/center?type=LEARNING_MAP" class="quick-access-item">
          <svg-icon icon-class="map" class="icon-container" />
          <span>学习地图</span>
        </router-link>
        <router-link to="#" class="quick-access-item">
        <!-- <router-link to="/toc/learning/center?type=SURVEY" class="quick-access-item"> -->
          <svg-icon icon-class="survey" class="icon-container" />
          <span>调研任务</span>
        </router-link>
      </div>

      <!-- 学习任务 -->
      <div class="section learning-tasks">
        <div class="section-header">
          <h2>学习任务</h2>
          <div class="task-filters">
            <a-radio-group v-model:value="taskType" button-style="solid" size="small">
              <a-radio-button value="train">培训</a-radio-button>
              <a-radio-button value="map">地图</a-radio-button>
              <!-- <a-radio-button value="exam">考试</a-radio-button> -->
            </a-radio-group>
          </div>
          <router-link to="/toc/learning/tasks" class="view-all">
            查看全部 <right-outlined />
          </router-link>
        </div>

        <div class="task-list" v-if="filteredTasks.length > 0">
          <div class="task-cards">
            <CourseCard v-for="task in filteredTasks.slice(0, 3)" :key="task.id" :item="task" :showProgress="true"
              :showSource="true" />
          </div>
        </div>
        <EmptyState v-else title="暂无学习任务" description="您当前没有需要完成的学习任务">
          <template #action>
            <a-button type="primary" @click="goToLearningCenter">去学习中心</a-button>
          </template>
        </EmptyState>
      </div>

      <!-- 推荐课程 -->
      <div class="section recommended-courses">
        <div class="section-header">
          <h2>推荐课程</h2>
          <router-link to="/toc/learning/center" class="view-all">
            查看全部 <right-outlined />
          </router-link>
        </div>

        <div class="course-list" v-if="recommendedCourses.length > 0">
          <div class="course-cards">
            <CourseCard v-for="course in recommendedCourses.slice(0, 3)" :key="course.id" :item="course"
              :showProgress="false" :showSource="false" />
          </div>
        </div>
        <EmptyState v-else title="暂无推荐课程" description="系统暂时没有为您推荐的课程" />
      </div>

      <!-- 排行榜 -->
      <!--      <div class="section rankings">-->
      <!--        <div class="section-header">-->
      <!--          <h2>{{ rankingType === 'all' ? '全员排行榜' : '部门排行榜' }}</h2>-->
      <!--          <div class="ranking-filters">-->
      <!--            <a-radio-group v-model:value="rankingMetric" button-style="solid" size="small">-->
      <!--              <a-radio-button value="credit">学分榜</a-radio-button>-->
      <!--              <a-radio-button value="duration">学时榜</a-radio-button>-->
      <!--            </a-radio-group>-->
      <!--          </div>-->
      <!--          <router-link to="/toc/rankings" class="view-all">-->
      <!--            查看全部 <right-outlined />-->
      <!--          </router-link>-->
      <!--        </div>-->

      <!--        <div class="ranking-tabs">-->
      <!--          <a-tabs v-model:activeKey="rankingType">-->
      <!--            <a-tab-pane key="all" tab="全员排行榜">-->
      <!--              <div class="ranking-list" v-if="allUserRanking.length > 0">-->
      <!--                <div class="ranking-users">-->
      <!--                  <div-->
      <!--                    v-for="(user, index) in allUserRanking.slice(0, 8)"-->
      <!--                    :key="user.userId"-->
      <!--                    class="ranking-user"-->
      <!--                  >-->
      <!--                    <div class="ranking-medal" v-if="index < 3">-->
      <!--                      <trophy-outlined v-if="index === 0" class="gold" />-->
      <!--                      <trophy-outlined v-else-if="index === 1" class="silver" />-->
      <!--                      <trophy-outlined v-else class="bronze" />-->
      <!--                    </div>-->
      <!--                    <a-avatar :src="user.avatar" />-->
      <!--                    <div class="ranking-user-info">-->
      <!--                      <div class="ranking-user-name">{{ user.nickname }}</div>-->
      <!--                      <div class="ranking-user-dept">{{ user.departmentName }}</div>-->
      <!--                    </div>-->
      <!--                    <div class="ranking-score">-->
      <!--                      {{ rankingMetric === 'credit' ? user.credit + ' 分' : formatStudyTime(user.studyDuration) }}-->
      <!--                    </div>-->
      <!--                  </div>-->
      <!--                </div>-->
      <!--              </div>-->
      <!--              <EmptyState-->
      <!--                v-else-->
      <!--                title="暂无排行数据"-->
      <!--                description="暂时没有排行榜数据"-->
      <!--              />-->
      <!--            </a-tab-pane>-->
      <!--            <a-tab-pane key="department" tab="部门排行榜">-->
      <!--              <div class="ranking-list" v-if="departmentRanking.length > 0">-->
      <!--                <div class="ranking-users">-->
      <!--                  <div-->
      <!--                    v-for="(user, index) in departmentRanking.slice(0, 8)"-->
      <!--                    :key="user.userId"-->
      <!--                    class="ranking-user"-->
      <!--                  >-->
      <!--                    <div class="ranking-medal" v-if="index < 3">-->
      <!--                      <trophy-outlined v-if="index === 0" class="gold" />-->
      <!--                      <trophy-outlined v-else-if="index === 1" class="silver" />-->
      <!--                      <trophy-outlined v-else class="bronze" />-->
      <!--                    </div>-->
      <!--                    <a-avatar :src="user.avatar" />-->
      <!--                    <div class="ranking-user-info">-->
      <!--                      <div class="ranking-user-name">{{ user.nickname }}</div>-->
      <!--                      <div class="ranking-user-dept">{{ user.departmentName }}</div>-->
      <!--                    </div>-->
      <!--                    <div class="ranking-score">-->
      <!--                      {{ rankingMetric === 'credit' ? user.credit + ' 分' : formatStudyTime(user.studyDuration) }}-->
      <!--                    </div>-->
      <!--                  </div>-->
      <!--                </div>-->
      <!--              </div>-->
      <!--              <EmptyState-->
      <!--                v-else-->
      <!--                title="暂无排行数据"-->
      <!--                description="暂时没有部门排行榜数据"-->
      <!--              />-->
      <!--            </a-tab-pane>-->
      <!--          </a-tabs>-->
      <!--        </div>-->
      <!--      </div>-->
    </div>

    <!-- 底部页脚 -->
    <FooterComponent />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  RightOutlined,
  LeftOutlined,
  TrophyOutlined
} from '@ant-design/icons-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import CourseCard from '@/components/common/CourseCard.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getHomeData, getBanners, getLearningTasks, getRankings } from '@/api/toc/home';
import SvgIcon from '@/components/common/SvgIcon.vue';

export default {
  name: 'HomePage',
  components: {
    HeaderComponent,
    FooterComponent,
    CourseCard,
    EmptyState,
    RightOutlined,
    LeftOutlined,
    TrophyOutlined,
    SvgIcon
  },
  setup() {
    const router = useRouter();

    // 轮播图数据
    const banners = ref([]);
    const currentBannerIndex = ref(0);

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      departmentName: '',
      totalCredit: 0,
      totalStudyDuration: 0,
      certificateCount: 0
    });

    // 用户成就
    const userAchievements = ref({
      completedTrainCount: 0,
      completedMapCount: 0,
      passedExamCount: 0
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

    // 排行榜
    const rankingType = ref('all');
    const rankingMetric = ref('credit');
    const allUserRanking = ref([]);
    const departmentRanking = ref([]);

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

    const goToBanner = (banner) => {
      window.open(banner.linkUrl, '_blank');
    };

    // 格式化学习时长
    const formatStudyTime = (minutes) => {
      if (!minutes) return '0 分钟';
      return `${minutes} 分钟`;
    };

    // 轮播图控制
    const goToSlide = (index) => {
      currentBannerIndex.value = index;
    };

    const nextSlide = () => {
      currentBannerIndex.value = (currentBannerIndex.value + 1) % banners.value.length;
    };

    const prevSlide = () => {
      currentBannerIndex.value = (currentBannerIndex.value - 1 + banners.value.length) % banners.value.length;
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/learning/center');
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
            // 假设考试任务也在这里
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

          // 设置排行榜数据
          if (data.rankings) {
            // 根据rankingMetric选择合适的排行榜数据
            updateRankingData(data.rankings);
          }

          // 设置用户成就
          userAchievements.value = {
            completedTrainCount: data.userInfo?.completedTrainCount || 0,
            completedMapCount: data.userInfo?.completedMapCount || 0,
            passedExamCount: data.userInfo?.passedExamCount || 0
          };
        }
      } catch (error) {
        console.error('获取首页数据失败:', error);
      }
    };

    // 获取轮播图数据
    const fetchBanners = async () => {
      try {
        const { data } = await getBanners({
          type: 'PC'
        });
        banners.value = data || [];
      } catch (error) {
        console.error('获取轮播图数据失败:', error);
      }
    };

    // 更新排行榜数据
    const updateRankingData = (rankings) => {
      if (rankingMetric.value === 'credit') {
        allUserRanking.value = rankings.courseCompletionRanking || [];
        departmentRanking.value = rankings.departmentRanking || [];
      } else {
        allUserRanking.value = rankings.studyDurationRanking || [];
        departmentRanking.value = rankings.departmentRanking || [];
      }
    };

    // 获取排行榜数据
    const fetchRankings = async () => {
      try {
        const { data } = await getRankings({
          type: rankingType.value,
          limit: 8
        });

        if (rankingType.value === 'all') {
          allUserRanking.value = data || [];
        } else {
          departmentRanking.value = data || [];
        }
      } catch (error) {
        console.error('获取排行榜数据失败:', error);
      }
    };

    // 组件挂载时获取数据
    onMounted(() => {
      fetchHomeData();
      fetchBanners();
    });

    return {
      banners,
      currentBannerIndex,
      userInfo,
      userAchievements,
      taskType,
      learningTasks,
      filteredTasks,
      recommendedCourses,
      rankingType,
      rankingMetric,
      allUserRanking,
      departmentRanking,
      formatStudyTime,
      goToSlide,
      nextSlide,
      prevSlide,
      goToLearningCenter,
      goToBanner
    };
  }
};
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  display: flex;
  flex-direction: column;
}

.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px;
  flex: 1;
}

/* 顶部区域：轮播图和用户信息 */
.top-section {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.carousel-container {
  flex: 1;
  position: relative;
  height: 384px;
  border-radius: 12px;
  overflow: hidden;
}

.carousel-item {
  height: 384px;
  position: relative;
  cursor: pointer;
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
  padding: 48px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
}

.carousel-title {
  color: #FFFFFF;
  font-size: 36px;
  font-weight: bold;
  margin: 0;
}

.carousel-indicators {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.indicator {
  width: 28px;
  height: 6px;
  background-color: #D8D8D8;
  border-radius: 3px;
  cursor: pointer;
}

.indicator.active {
  background-color: #FFFFFF;
}

.carousel-controls {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
}

.control {
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #333;
}

.user-info-card {
  width: 387px;
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0px 4px 6px -1px rgba(0, 0, 0, 0.1), 0px 2px 4px -2px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.user-profile {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.user-details {
  margin-left: 16px;
}

.user-details h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.user-details p {
  margin: 0;
  color: #6B7280;
  font-size: 16px;
}

.user-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #1890FF;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
}

.user-achievements {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.achievement-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.achievement-label {
  color: #4B5563;
  font-size: 16px;
}

.achievement-value {
  font-size: 16px;
  font-weight: 500;
}

/* 快捷入口 */
.quick-access {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.quick-access-item {
  width: 227px;
  height: 152px;
  background-color: #FFFFFF;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  color: #374151;
  box-shadow: 0px 4px 6px -1px rgba(0, 0, 0, 0.1), 0px 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.icon-container {
  width: 64px;
  height: 64px;
  background-color: rgba(79, 70, 229, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  font-size: 24px;
  color: #1890FF;
}


.quick-access-item span {
  font-size: 16px;
  font-weight: 500;
}

/* 通用区块样式 */
.section {
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0px 4px 6px -1px rgba(0, 0, 0, 0.1), 0px 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: bold;
}

.view-all {
  color: #1890FF;
  text-decoration: none;
  font-size: 16px;
  display: flex;
  align-items: center;
}

/* 学习任务 */
.task-cards,
.course-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* 排行榜 */
.ranking-users {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 30px;
}

.ranking-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
}

.ranking-medal {
  position: absolute;
  top: 0;
  font-size: 16px;
}

.gold {
  color: #EAB308;
}

.silver {
  color: #9CA3AF;
}

.bronze {
  color: #CA8A04;
}

.ranking-user-info {
  margin-top: 8px;
}

.ranking-user-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.ranking-user-dept {
  font-size: 14px;
  color: #6B7280;
}

.ranking-score {
  font-size: 18px;
  font-weight: bold;
  color: #1890FF;
  margin-top: 8px;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .top-section {
    flex-direction: column;
  }

  .user-info-card {
    width: 100%;
  }

  .quick-access {
    flex-wrap: wrap;
    gap: 16px;
  }

  .quick-access-item {
    width: calc(50% - 8px);
  }

  .task-cards,
  .course-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .ranking-users {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {

  .task-cards,
  .course-cards {
    grid-template-columns: 1fr;
  }

  .ranking-users {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
