<template>
  <div class="train-detail-page">
    <!-- 头部导航 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <!-- 主体内容 -->
    <div class="main-container">

      <!-- 背景区域 -->
      <div class="background-area">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-container">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/toc/learning/center">学习中心</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>{{ trainDetail.name }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="background-image"></div>
        <div class="background-gradient"></div>
      </div>

      <!-- 主体内容区 -->
      <div class="content-container">
        <!-- 培训详情信息 -->
        <div class="train-info">
          <h1 class="train-title">{{ trainDetail.name }}</h1>
          <div class="train-meta">
            <span class="train-type">
              <span class="train-type-text">培训</span> · 共{{ trainDetail.contents ? trainDetail.contents.length : 0 }}门课程
              {{ trainDetail.learners ? trainDetail.learners.total : 0 }}人在学，
              {{ trainDetail.learners ? trainDetail.learners.completed : 0 }}人已完成
            </span>
            <div class="share-btn">
              <svg width="21" height="21" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M20.7071 0.292922C21.006 0.591755 21.0847 1.04618 20.9038 1.42811C20.9038 1.42811 11.9038 20.4281 11.9038 20.4281C11.7229 20.8098 11.3218 21.0368 10.9015 20.9952C10.4812 20.9535 10.1323 20.6523 10.0299 20.2426C10.0299 20.2426 8.1754 12.8246 8.1754 12.8246C8.1754 12.8246 0.757489 10.9702 0.757489 10.9702C0.347726 10.8677 0.0465113 10.5189 0.00489156 10.0986C-0.0367282 9.67825 0.190223 9.2771 0.571939 9.09629C0.571939 9.09629 19.5719 0.0962906 19.5719 0.0962906C19.9539 -0.0846246 20.4083 -0.00591126 20.7071 0.292922C20.7071 0.292922 20.7071 0.292922 20.7071 0.292922ZM3.95337 9.70759C3.95337 9.70759 9.24256 11.0299 9.24256 11.0299C9.60085 11.1195 9.8806 11.3992 9.97017 11.7575C9.97017 11.7575 11.2925 17.0467 11.2925 17.0467C11.2925 17.0467 17.8976 3.10241 17.8976 3.10241C17.8976 3.10241 3.95337 9.70759 3.95337 9.70759C3.95337 9.70759 3.95337 9.70759 3.95337 9.70759Z"
                  fill="#0256FF" />
              </svg>
              <span class="share-text">分享</span>
            </div>
          </div>
          <p class="train-description">{{ trainDetail.introduction }}</p>
          <div class="train-attributes">
            <div class="attribute-item">
              <svg width="20" height="16" viewBox="0 0 20 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M9.97274 14.0078Q9.59891 14.0078 9.22508 13.8828L0.502336 10.7266Q0.0350467 10.5078 0.00389408 10.0078Q0.0350467 9.50781 0.502336 9.28906L2.30919 8.63281Q1.53037 7.41406 1.49922 5.88281L1.49922 5.00781Q1.40576 3.63281 0.813863 2.47656Q0.502336 1.88281 0.097352 1.32031Q-0.0584112 1.10156 0.0350467 0.851562Q0.128505 0.601562 0.377726 0.507812L2.3715 0.0078125Q2.58956 -0.0234375 2.77648 0.0703125Q2.93224 0.195312 2.99455 0.414062Q3.30607 2.47656 2.93224 3.78906Q2.77648 4.47656 2.49611 5.10156L2.49611 5.88281Q2.52726 7.32031 3.36838 8.44531Q3.96028 9.16406 4.89486 9.53906L9.78583 11.4766Q10.222 11.6016 10.44 11.1953Q10.5646 10.7578 10.1597 10.5391L5.26869 8.63281Q4.67679 8.38281 4.27181 7.94531L9.22508 6.13281Q9.59891 6.00781 9.97274 6.00781Q10.3466 6.00781 10.7204 6.13281L19.4431 9.28906Q19.9104 9.50781 19.9416 10.0078Q19.9104 10.5078 19.4431 10.7266L10.7204 13.8828Q10.3466 14.0078 9.97274 14.0078ZM3.99143 2.25781Q4.05374 1.38281 5.73598 0.726562Q7.41822 0.0390625 9.97274 0.0078125Q12.5273 0.0390625 14.2095 0.726562Q15.8917 1.38281 15.9541 2.25781L15.4868 6.78906L11.0631 5.19531Q10.5335 5.00781 9.97274 5.00781Q9.41199 5.00781 8.91355 5.19531L4.45872 6.78906L3.99143 2.25781Z"
                  fill="#1890FF" />
              </svg>
              <span>{{ trainDetail.credit }} 学分</span>
            </div>
            <div class="attribute-item"
              v-if="trainDetail.userProgress && trainDetail.userProgress.status === 'completed'">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M8 0Q10.1875 0.03125 12 1.0625Q13.8125 2.125 14.9375 4Q16 5.90625 16 8Q16 10.0938 14.9375 12Q13.8125 13.875 12 14.9375Q10.1875 15.9688 8 16Q5.8125 15.9688 4 14.9375Q2.1875 13.875 1.0625 12Q0 10.0938 0 8Q0 5.90625 1.0625 4Q2.1875 2.125 4 1.0625Q5.8125 0.03125 8 0ZM11.5312 9.46875L7.53125 5.46875L11.5312 9.46875L7.53125 5.46875Q7 5.03125 6.46875 5.46875L4.46875 7.46875Q4.03125 8 4.46875 8.53125Q5 8.96875 5.53125 8.53125L7 7.0625L10.4688 10.5312Q11 10.9688 11.5312 10.5312Q11.9688 10 11.5312 9.46875Z"
                  fill="#4ADE80" />
              </svg>
              <span>已完成: {{ formatDate(trainDetail.userProgress.completionTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 培训内容列表 -->
        <div class="train-content">
          <h2 class="content-title">目录：</h2>

          <!-- 内容筛选标签 -->
          <div class="content-tabs">
            <div class="tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
              全部
            </div>
            <div class="tab-item" :class="{ active: activeTab === 'completed' }" @click="activeTab = 'completed'">
              已完成 ({{ completedCount }})
            </div>
            <div class="tab-item" :class="{ active: activeTab === 'learning' }" @click="activeTab = 'learning'">
              进行中 ({{ learningCount }})
            </div>
            <div class="tab-item" :class="{ active: activeTab === 'not_started' }" @click="activeTab = 'not_started'">
              未开始 ({{ notStartedCount }})
            </div>
            <!-- <div class="view-all" @click="activeTab = 'all'">查看全部></div> -->
          </div>

          <!-- 内容列表 -->
          <div class="content-list">
            <div v-for="(item, index) in filteredContents" :key="index" class="content-item" @click="goToContent(item)">
              <!-- 内容图标/缩略图 -->
              <div class="content-icon">
                <template v-if="item.type === 'video'">
                  <div class="video-thumbnail" :style="{ backgroundImage: `url(${item.coverImage || ''})` }">
                    <div class="play-icon">
                      <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M0 7Q0.0273438 8.91406 0.929687 10.5Q1.85937 12.0859 3.5 13.0703Q5.16797 14 7 14Q8.83203 14 10.5 13.0703Q12.1406 12.0859 13.0703 10.5Q13.9727 8.91406 14 7Q13.9727 5.08594 13.0703 3.5Q12.1406 1.91406 10.5 0.929687Q8.83203 0 7 0Q5.16797 0 3.5 0.929687Q1.85937 1.91406 0.929687 3.5Q0.0273438 5.08594 0 7ZM5.14062 9.98047Q4.8125 9.78906 4.8125 9.40625L4.8125 4.59375Q4.8125 4.21094 5.14062 4.01953Q5.49609 3.85547 5.82422 4.04688L9.76172 6.45312Q10.0625 6.64453 10.0625 7Q10.0625 7.35547 9.76172 7.57422L5.82422 9.98047Q5.49609 10.1445 5.14062 9.98047Z"
                          fill="white" />
                      </svg>
                    </div>
                  </div>
                </template>
                <template v-else-if="item.type === 'document'">
                  <div class="document-icon">
                    <svg width="15" height="20" viewBox="0 0 15 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path
                        d="M2.5 20Q1.44531 19.9609 0.742188 19.2578Q0.0390625 18.5547 0 17.5L0 2.5Q0.0390625 1.44531 0.742188 0.742188Q1.44531 0.0390625 2.5 0L12.5 0Q13.5547 0.0390625 14.2578 0.742188Q14.9609 1.44531 15 2.5L15 13.75L10 13.75Q9.45312 13.75 9.10156 14.1016Q8.75 14.4531 8.75 15L8.75 20L2.5 20ZM10 20L10 15L10 20L10 15L15 15L10 20ZM4.375 10L10.625 10L4.375 10L10.625 10Q11.2109 9.96094 11.25 9.375Q11.2109 8.78906 10.625 8.75L4.375 8.75Q3.78906 8.78906 3.75 9.375Q3.78906 9.96094 4.375 10ZM4.375 7.5L10.625 7.5L4.375 7.5L10.625 7.5Q11.2109 7.46094 11.25 6.875Q11.2109 6.28906 10.625 6.25L4.375 6.25Q3.78906 6.28906 3.75 6.875Q3.78906 7.46094 4.375 7.5ZM4.375 5L10.625 5L4.375 5L10.625 5Q11.2109 4.96094 11.25 4.375Q11.2109 3.78906 10.625 3.75L4.375 3.75Q3.78906 3.78906 3.75 4.375Q3.78906 4.96094 4.375 5Z"
                        fill="#64748B" />
                    </svg>
                  </div>
                </template>
                <template v-else>
                  <div class="content-thumbnail" :style="{ backgroundImage: `url(${item.coverImage || ''})` }"></div>
                </template>
              </div>

              <!-- 内容信息 -->
              <div class="content-info">
                <div class="content-header">
                  <h3 class="content-name">{{ item.title }}</h3>
                  <span class="content-required"
                    :class="{ 'required': item.isRequired === 1, 'optional': item.isRequired === 0 }">
                    {{ item.isRequired === 1 ? '必修' : '选修' }}
                  </span>
                </div>
                <p class="content-type">
                  {{ getContentTypeText(item.type) }}
                  {{ item.type === 'document' ? '· ' + getFileSizeText(item) : '' }}
                  {{ item.type === 'video' ? '· ' + getDurationText(item) : '' }}
                </p>
                <div class="progress-bar">
                  <div class="progress-track"></div>
                  <div class="progress-fill" :style="{ width: `${item.progress || 0}%` }"></div>
                </div>
                <div class="progress-text">
                  {{ getProgressText(item) }}
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <empty-state v-if="filteredContents.length === 0" title="暂无内容" description="该培训暂无相关内容" />
          </div>

          <!-- 学习人员区域 -->
          <div class="learners-section">
            <h3 class="learners-title">学习人员 <span class="learner-count">({{ trainDetail.learners ? trainDetail.learners.total : 0 }})</span></h3>

            <!-- 人员筛选标签 -->
            <div class="learners-tabs">
              <div class="tab-item" :class="{ active: activeLearnerTab === 'all' }" @click="activeLearnerTab = 'all'">
                全部 ({{ trainDetail.learners ? trainDetail.learners.total : 0 }})
              </div>
              <div class="tab-item" :class="{ active: activeLearnerTab === 'completed' }" @click="activeLearnerTab = 'completed'">
                已完成 ({{ trainDetail.learners ? trainDetail.learners.completed : 0 }})
              </div>
              <div class="tab-item" :class="{ active: activeLearnerTab === 'learning' }" @click="activeLearnerTab = 'learning'">
                进行中 ({{ trainDetail.learners ? trainDetail.learners.learning : 0 }})
              </div>
              <div class="tab-item" :class="{ active: activeLearnerTab === 'not_started' }" @click="activeLearnerTab = 'not_started'">
                未开始 ({{ trainDetail.learners ? trainDetail.learners.notStart : 0 }})
              </div>
            </div>

            <div class="learners-list">
              <a-empty v-if="!filteredLearners.length" description="暂无学习人员" />
              <div v-else class="learners-grid">
                <div v-for="(learner, index) in filteredLearners" :key="index" class="learner-item">
                  <a-tooltip :title="learner.nickname + (learner.department ? ' - ' + learner.department : '')">
                    <a-badge :dot="learner.status === 'completed'" :color="getLearnerStatusColor(learner.status)">
                      <a-avatar :src="learner.avatar" v-if="learner.avatar">
                        <template #icon v-if="!learner.avatar">
                          <UserOutlined />
                        </template>
                      </a-avatar>
                      <a-avatar v-else>
                        <template #icon>
                          <UserOutlined />
                        </template>
                      </a-avatar>
                    </a-badge>
                  </a-tooltip>
                  <div class="learner-name">{{ learner.nickname }}</div>
                  <div class="learner-status">
                    <a-tag :color="getLearnerStatusTagColor(learner.status)">
                      {{ getLearnerStatusText(learner.status) }}
                    </a-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <button class="action-button" @click="startLearning">
          {{ getActionButtonText() }}
        </button>
      </div>
    </div>

    <!-- 页脚 -->
    <footer-component />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getTrainDetail } from '@/api/toc/train';
import { recordLearningProgress } from '@/api/toc/learning';
import { UserOutlined } from '@ant-design/icons-vue';

export default defineComponent({
  name: 'TrainDetailPage',
  components: {
    HeaderComponent,
    FooterComponent,
    EmptyState,
    UserOutlined
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const trainDetail = ref({});
    const loading = ref(false);
    const activeTab = ref('all');
    const activeLearnerTab = ref('all');

    // 获取培训详情
    const fetchTrainDetail = async () => {
      try {
        loading.value = true;
        const id = route.params.id;
        if (!id) {
          message.error('培训ID不能为空');
          return;
        }

        const res = await getTrainDetail(id);
        if (res.code === 200 && res.data) {
          trainDetail.value = res.data;

          recordProgress(trainDetail.value.id);
        } else {
          message.error(res.message || '获取培训详情失败');
        }
      } catch (error) {
        console.error('获取培训详情失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 计算已完成的内容数量
    const completedCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => item.status === 'completed').length;
    });

    // 计算进行中的内容数量
    const learningCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => item.status === 'learning').length;
    });

    // 计算未开始的内容数量
    const notStartedCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => item.status === 'not_started').length;
    });

    // 根据当前选中的标签筛选内容
    const filteredContents = computed(() => {
      if (!trainDetail.value.contents) return [];
      if (activeTab.value === 'all') return trainDetail.value.contents;
      return trainDetail.value.contents.filter(item => item.status === activeTab.value);
    });

    // 根据当前选中的标签筛选学习人员
    const filteredLearners = computed(() => {
      if (!trainDetail.value.learners) return [];

      if (activeLearnerTab.value === 'all') {
        // 合并所有人员列表
        const allLearners = [];
        if (trainDetail.value.learners.completedList) {
          allLearners.push(...trainDetail.value.learners.completedList);
        }
        if (trainDetail.value.learners.learningList) {
          allLearners.push(...trainDetail.value.learners.learningList);
        }
        if (trainDetail.value.learners.notStartList) {
          allLearners.push(...trainDetail.value.learners.notStartList);
        }
        return allLearners;
      } else if (activeLearnerTab.value === 'completed') {
        return trainDetail.value.learners.completedList || [];
      } else if (activeLearnerTab.value === 'learning') {
        return trainDetail.value.learners.learningList || [];
      } else if (activeLearnerTab.value === 'not_started') {
        return trainDetail.value.learners.notStartList || [];
      }

      return [];
    });

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    };

    // 获取内容类型文本
    const getContentTypeText = (type) => {
      const typeMap = {
        'video': '视频课程',
        'document': '文档',
        'article': '文章',
        'series': '系列课程',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '调研',
        'CONTENT': '课程'
      };
      return typeMap[type] || '课程';
    };

    // 获取文件大小文本
    const getFileSizeText = (item) => {
      return item.fileSize || '2.1MB';
    };

    // 获取时长文本
    const getDurationText = (item) => {
      return item.duration || '45分钟';
    };

    // 获取进度文本
    const getProgressText = (item) => {
      if (item.status === 'completed') return '已完成';
      if (item.status === 'learning') return `${item.progress || 0}%`;
      return '未开始';
    };

    // 获取操作按钮文本
    const getActionButtonText = () => {
      if (!trainDetail.value.userProgress) return '开始学习';
      if (trainDetail.value.userProgress.status === 'completed') return '已学完';
      return '继续学习';
    };

    // 跳转到内容详情页
    const goToContent = (item) => {
      if (!item || !item.contentId) {
        message.warning('内容不存在');
        return;
      }

      // 根据内容类型跳转到不同的页面
      if (item.contentType === 'COURSE') {
        router.push(`/toc/course/detail/${item.contentId}`);
      } else if (item.contentType === 'EXAM') {
        router.push(`/toc/exam/detail/${item.contentId}`);
      } else if (item.contentType === 'ASSIGNMENT') {
        router.push(`/toc/assignment/detail/${item.contentId}`);
      } else if (item.contentType === 'SURVEY') {
        router.push(`/toc/survey/detail/${item.contentId}`);
      } else {
        message.warning('暂不支持该内容类型');
      }
    };

    // 开始学习
    const startLearning = async () => {
      try {
        // 找到第一个未完成的内容
        const firstUnfinishedContent = trainDetail.value.contents?.find(
          item => item.status !== 'completed'
        );

        if (!firstUnfinishedContent) {
          message.info('所有内容已学习完成');
          return;
        }

        // 跳转到内容详情页
        goToContent(firstUnfinishedContent);
      } catch (error) {
        console.error('开始学习失败:', error);
      }
    };

    const recordProgress = async (contentId) => {
      // 记录学习进度
      await recordLearningProgress({
        parentType: '',
        contentType: 'TRAIN',
        contentId: contentId,
        progress: 0,
        duration: 0,
        parentId: trainDetail.value.id
      });
    }

    // 获取学习人员状态颜色
    const getLearnerStatusColor = (status) => {
      if (status === 'completed') return '#52c41a';
      if (status === 'learning') return '#1890ff';
      return '#faad14';
    };

    // 获取学习人员状态标签颜色
    const getLearnerStatusTagColor = (status) => {
      if (status === 'completed') return 'success';
      if (status === 'learning') return 'processing';
      return 'warning';
    };

    // 获取学习人员状态文本
    const getLearnerStatusText = (status) => {
      if (status === 'completed') return '已完成';
      if (status === 'learning') return '学习中';
      return '未开始';
    };

    onMounted(() => {
      fetchTrainDetail();
    });

    return {
      trainDetail,
      loading,
      activeTab,
      activeLearnerTab,
      completedCount,
      learningCount,
      notStartedCount,
      filteredContents,
      filteredLearners,
      formatDate,
      getContentTypeText,
      getFileSizeText,
      getDurationText,
      getProgressText,
      getActionButtonText,
      getLearnerStatusColor,
      getLearnerStatusTagColor,
      getLearnerStatusText,
      goToContent,
      startLearning
    };
  }
});
</script>

<style scoped>
.train-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F9FAFB;
}

.main-container {
  flex: 1;
  position: relative;
}


.breadcrumb-container {
  position: relative;
  padding: 12px 16px;
  z-index: 2;
  width: 100%;
  max-width: 1142px;
  margin: 0 auto;
}

:deep(.ant-breadcrumb-link) {
  color: #fff !important;
  font-weight: bold;

}

:deep(.ant-breadcrumb-link a) {
  color: #fff !important;
  font-weight: bold;

}

:deep(.ant-breadcrumb-separator) {
  color: #fff !important;
  font-weight: bold;
}

.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 810px;
  background-image: url('https://plus.unsplash.com/premium_photo-1661778490723-371305b4fb06?q=80&w=3540&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
}

.background-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 847px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, #FFFCFC 80%, #F9FAFB 98%);
}

.content-container {
  position: relative;
  z-index: 1;
  max-width: 1142px;
  margin: 0 auto;
}

.train-info {
  margin-bottom: 40px;
  background-color: #fff;
  padding: 32px;
  border-radius: 8px;
}

.train-title {
  font-size: 30px;
  font-weight: bold;
  color: #050404;
  margin-bottom: 16px;
  line-height: 36px;
}

.train-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.train-type {
  font-size: 18px;
  line-height: 28px;
}

.train-type-text {
  color: #EF4444;
}

.share-btn {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.share-text {
  margin-left: 5px;
  font-size: 20px;
  color: #2563EB;
}

.train-description {
  font-size: 18px;
  line-height: 28px;
  color: #050404;
  opacity: 0.9;
  margin-bottom: 20px;
}

.train-attributes {
  display: flex;
  gap: 24px;
  margin-top: 20px;
}

.attribute-item {
  display: flex;
  align-items: center;
  font-size: 16px;
  color: #050404;
}

.attribute-item svg {
  margin-right: 8px;
}

.train-content {
  background-color: #FFFFFF;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 32px;
}

.content-title {
  font-size: 24px;
  font-weight: bold;
  color: #050404;
  margin-bottom: 24px;
}

.content-tabs {
  display: flex;
  border-bottom: 1px solid #E5E7EB;
  margin-bottom: 24px;
  position: relative;
}

.tab-item {
  padding: 8px 0;
  margin-right: 32px;
  font-size: 16px;
  color: #6B7280;
  cursor: pointer;
  position: relative;
}

.tab-item.active {
  color: #1890FF;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #3B82F6;
}

.view-all {
  position: absolute;
  right: 0;
  top: 8px;
  font-size: 16px;
  color: #6B7280;
  cursor: pointer;
}

.content-list {
  margin-bottom: 32px;
}

.content-item {
  display: flex;
  padding: 16px;
  border-radius: 12px;
  background-color: #FFFFFF;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.content-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.content-icon {
  width: 92px;
  height: 52px;
  margin-right: 16px;
  border-radius: 5px;
  overflow: hidden;
  background-color: #F3F4F6;
  display: flex;
  justify-content: center;
  align-items: center;
}

.video-thumbnail {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 22px;
  height: 21px;
  background-color: rgba(5, 4, 4, 0.41);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.content-thumbnail {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.content-info {
  flex: 1;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.content-name {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  margin: 0;
}

.content-required {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}

.required {
  background-color: #FEF2F2;
  color: #EF4444;
}

.optional {
  background-color: #F9FAFB;
  color: #6B7280;
}

.content-type {
  font-size: 14px;
  color: #6B7280;
  margin: 8px 0;
}

.progress-bar {
  height: 4px;
  background-color: #F3F4F6;
  border-radius: 9999px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-track {
  height: 100%;
  width: 100%;
  background-color: #F3F4F6;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3B82F6 0%, #60A5FA 100%);
  border-radius: 9999px;
  position: relative;
  top: -4px;
}

.progress-text {
  text-align: right;
  font-size: 18px;
  color: #6B7280;
}

.comments-section {
  margin-top: 40px;
}

.comments-title {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 16px;
}

.comments-list {
  margin-bottom: 16px;
}

.comment-item {
  display: flex;
  margin-bottom: 16px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #E5E7EB;
  margin-right: 16px;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  margin-right: 8px;
}

.comment-time {
  font-size: 14px;
  color: #6B7280;
}

.comment-text {
  font-size: 16px;
  color: #4B5563;
  margin: 0;
}

.comment-input-container {
  display: flex;
  margin-bottom: 32px;
}

.user-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background-color: #E5E7EB;
  margin-right: 8px;
}

.comment-input {
  flex: 1;
  position: relative;
  background-color: #EFF6FF;
  border-radius: 5px;
  padding: 8px 16px;
}

.comment-input input {
  width: 100%;
  height: 40px;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #000000;
}

.comment-input input::placeholder {
  color: #9CA3AF;
}

.send-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}

.learners-section {
  margin-top: 40px;
  border-top: 1px solid #e5e7eb;
  padding-top: 24px;
}

.learners-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.learner-count {
  font-size: 14px;
  color: #6b7280;
  font-weight: normal;
  margin-left: 8px;
}

.learners-tabs {
  display: flex;
  border-bottom: 1px solid #E5E7EB;
  margin-bottom: 24px;
  position: relative;
}

.learners-list {
  position: relative;
}

.learners-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 16px;
}

.learner-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background-color: #f9fafb;
}

.learner-item:hover {
  background-color: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.learner-item .ant-avatar {
  margin-bottom: 8px;
  background: #1890ff;
}

.learner-name {
  font-size: 14px;
  color: #3d3d3d;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  margin-bottom: 4px;
}

.learner-status {
  margin-top: 4px;
}

.learner-item .ant-tag {
  margin: 0 !important;
}

.action-bar {
  position: sticky;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: #FFFFFF;
  border-top: 1px solid #E5E7EB;
  padding: 24px;
  display: flex;
  justify-content: center;
  z-index: 10;
}

.action-button {
  width: 100%;
  max-width: 1078px;
  height: 48px;
  background-color: #1890FF;
  color: #FFFFFF;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.action-button:hover {
  background-color: #4338CA;
}
</style>
