<template>
  <div class="train-detail-mobile-page">
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 培训详情信息 -->
        <div class="train-info-card">
          <h1 class="train-title">{{ trainDetail.name }}</h1>
          <div class="train-meta">
            <span class="train-type">
              <span class="train-type-text">培训</span> ·
              共{{ trainDetail.contents ? trainDetail.contents.length : 0 }}门课程
            </span>
            <div class="train-learners">
              {{ trainDetail.learners ? trainDetail.learners.total : 0 }}人在学，
              {{ trainDetail.learners ? trainDetail.learners.completed : 0 }}人已完成
            </div>
          </div>
          <p class="train-description">{{ trainDetail.introduction }}</p>
          <div class="train-attributes">
            <div class="attribute-item">
              <trophy-outlined />
              <span>{{ trainDetail.credit }} 学分</span>
            </div>
            <div class="attribute-item"
              v-if="trainDetail.userProgress && trainDetail.userProgress.status === 'completed'">
              <check-circle-outlined />
              <span>已完成: {{ formatDate(trainDetail.userProgress.completionTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 培训内容列表 -->
        <div class="train-content">
          <div class="content-header">
            <h2 class="content-title">目录</h2>

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
            </div>
          </div>

          <!-- 内容列表 -->
          <div class="content-list">
            <div v-for="(item, index) in filteredContents" :key="index" class="content-item" @click="goToContent(item)">
              <!-- 内容图标/缩略图 -->
              <div class="content-icon">
                <template v-if="item.type === 'video'">
                  <div class="video-thumbnail" :style="{ backgroundImage: `url(${item.coverImage || ''})` }">
                    <div class="play-icon">
                      <play-circle-outlined />
                    </div>
                  </div>
                </template>
                <template v-else-if="item.type === 'document'">
                  <div class="document-icon">
                    <file-outlined />
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
            <EmptyState v-if="filteredContents.length === 0" title="暂无内容" description="该培训暂无相关内容" />
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-actions">
      <a-button type="primary" block @click="startLearning">
        {{ getActionButtonText() }}
      </a-button>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getTrainDetail } from '@/api/toc/train';
import { recordLearningProgress } from '@/api/toc/learning';
import {
  TrophyOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  FileOutlined
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'TrainDetailMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    TrophyOutlined,
    CheckCircleOutlined,
    PlayCircleOutlined,
    FileOutlined
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const trainDetail = ref({});
    const loading = ref(false);
    const activeTab = ref('all');

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

    // 记录学习进度
    const recordProgress = async (trainId) => {
      try {
        await recordLearningProgress({
          parentType: '',
          contentType: 'TRAIN',
          contentId: trainId,
          progress: 0,
          duration: 0,
          parentId: trainDetail.value.id
        });
      } catch (error) {
        console.error('记录学习进度失败:', error);
      }
    };

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      return dayjs(dateStr).format('YYYY-MM-DD HH:mm');
    };

    // 获取内容类型文本
    const getContentTypeText = (type) => {
      const typeMap = {
        'video': '视频',
        'series': '系列课',
        'document': '文档',
        'article': '文章',
        'course': '课程',
        'exam': '考试'
      };
      return typeMap[type] || type;
    };

    // 获取文件大小文本
    const getFileSizeText = (item) => {
      if (!item.fileSize) return '';

      const size = parseInt(item.fileSize);
      if (size < 1024) {
        return `${size}B`;
      } else if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)}KB`;
      } else {
        return `${(size / (1024 * 1024)).toFixed(1)}MB`;
      }
    };

    // 获取视频时长文本
    const getDurationText = (item) => {
      if (!item.duration) return '';

      const duration = parseInt(item.duration);
      const minutes = Math.floor(duration / 60);
      const seconds = duration % 60;

      return `${minutes}分${seconds}秒`;
    };

    // 获取进度文本
    const getProgressText = (item) => {
      if (item.status === 'completed') {
        return '已完成';
      } else if (item.progress && item.progress > 0) {
        return `已学习 ${item.progress}%`;
      } else {
        return '未开始';
      }
    };

    // 获取按钮文本
    const getActionButtonText = () => {
      if (!trainDetail.value.userProgress) return '开始学习';

      if (trainDetail.value.userProgress.status === 'completed') {
        return '再学一次';
      } else if (trainDetail.value.userProgress.status === 'learning') {
        return '继续学习';
      } else {
        return '开始学习';
      }
    };

    // 开始学习
    const startLearning = () => {
      if (!trainDetail.value.contents || trainDetail.value.contents.length === 0) {
        message.warning('该培训暂无学习内容');
        return;
      }

      // 如果有进行中的内容，跳转到该内容
      const learningContent = trainDetail.value.contents.find(item => item.status === 'learning');
      if (learningContent) {
        goToContent(learningContent);
        return;
      }

      // 否则跳转到第一个内容
      goToContent(trainDetail.value.contents[0]);
    };

    // 跳转到内容详情
    const goToContent = (content) => {
      if (!content) return;

      const typeRouteMap = {
        'COURSE': `/toc/mobile/course/${content.contentId}`,
        'EXAM': `/toc/mobile/exam/${content.contentId}`,
        'ASSIGNMENT': `/toc/mobile/assignment/${content.contentId}`,
        'SURVEY': `/toc/mobile/survey/${content.contentId}`,
      };

      const route = typeRouteMap[content.contentType];
      if (route) {
        router.push(route);
      } else {
        message.warning('暂不支持查看此类型的内容');
      }
    };

    // 计算已完成内容数量
    const completedCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => item.status === 'completed').length;
    });

    // 计算进行中内容数量
    const learningCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => item.status === 'learning').length;
    });

    // 计算未开始内容数量
    const notStartedCount = computed(() => {
      if (!trainDetail.value.contents) return 0;
      return trainDetail.value.contents.filter(item => !item.status || item.status === 'not_started').length;
    });

    // 根据标签筛选内容
    const filteredContents = computed(() => {
      if (!trainDetail.value.contents) return [];

      if (activeTab.value === 'all') {
        return trainDetail.value.contents;
      } else if (activeTab.value === 'completed') {
        return trainDetail.value.contents.filter(item => item.status === 'completed');
      } else if (activeTab.value === 'learning') {
        return trainDetail.value.contents.filter(item => item.status === 'learning');
      } else if (activeTab.value === 'not_started') {
        return trainDetail.value.contents.filter(item => !item.status || item.status === 'not_started');
      }

      return trainDetail.value.contents;
    });

    onMounted(() => {
      fetchTrainDetail();
    });

    return {
      trainDetail,
      loading,
      activeTab,
      completedCount,
      learningCount,
      notStartedCount,
      filteredContents,
      formatDate,
      getContentTypeText,
      getFileSizeText,
      getDurationText,
      getProgressText,
      getActionButtonText,
      startLearning,
      goToContent
    };
  }
});
</script>

<style scoped>
.train-detail-mobile-page {
  padding-bottom: 140px;
}

.main-content {
  padding: 16px;
}

.train-info-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.train-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 12px;
  line-height: 1.4;
}

.train-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.train-type-text {
  color: #1890FF;
  font-weight: 500;
}

.train-description {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 16px;
}

.train-attributes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.attribute-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.attribute-item .anticon {
  margin-right: 6px;
  color: #1890FF;
}

.train-content {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.content-header {
  margin-bottom: 16px;
}

.content-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
}

.content-tabs {
  display: flex;
  overflow-x: auto;
  gap: 12px;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;
}

.tab-item {
  white-space: nowrap;
  padding: 6px 12px;
  border-radius: 16px;
  background-color: #f5f5f5;
  font-size: 12px;
  color: #666;
}

.tab-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-item {
  display: flex;
  background-color: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
}

.content-icon {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  position: relative;
}

.video-thumbnail,
.content-thumbnail {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-color: #eee;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.document-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  color: #666;
}

.document-icon .anticon {
  font-size: 24px;
}

.content-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.content-name {
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.content-required {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 8px;
  flex-shrink: 0;
}

.content-required.required {
  background-color: #fff1f0;
  color: #ff4d4f;
}

.content-required.optional {
  background-color: #f0f5ff;
  color: #1890ff;
}

.content-type {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.progress-bar {
  height: 4px;
  background-color: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-track {
  height: 100%;
  background-color: #f0f0f0;
}

.progress-fill {
  height: 100%;
  background-color: #1890ff;
  border-radius: 2px;
  position: relative;
  top: -4px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  margin-top: auto;
}

.bottom-actions {
  position: fixed;
  bottom: 65px;
  left: 0;
  right: 0;
  padding: 16px;
  background-color: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
}
</style>