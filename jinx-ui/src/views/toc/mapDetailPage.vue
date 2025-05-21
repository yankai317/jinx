<template>
  <div class="map-detail-page">
    <!-- 顶部导航 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <div class="main-content">
      <!-- 学习地图标题和进度 -->
      <div class="map-header">
        <h1 class="map-title">{{ mapDetail.name }}</h1>
        <div class="map-progress-container">
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div class="progress-bar-inner" :style="{ width: progressPercentage + '%' }"></div>
            </div>
            <span class="progress-text">学习进度 {{ progressPercentage }}%</span>
            <span class="study-time">预计学习时间：{{ formatStudyTime(totalStudyTime) }}</span>
          </div>
          <!-- <div class="share-button" @click="showShareModal">
            <svg width="21" height="21" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M20.7071 0.292922C21.006 0.591755 21.0847 1.04618 20.9038 1.42811C20.9038 1.42811 11.9038 20.4281 11.9038 20.4281C11.7229 20.8098 11.3218 21.0368 10.9015 20.9952C10.4812 20.9535 10.1323 20.6523 10.0299 20.2426C10.0299 20.2426 8.1754 12.8246 8.1754 12.8246C8.1754 12.8246 0.757489 10.9702 0.757489 10.9702C0.347726 10.8677 0.0465113 10.5189 0.00489156 10.0986C-0.0367282 9.67825 0.190223 9.2771 0.571939 9.09629C0.571939 9.09629 19.5719 0.0962906 19.5719 0.0962906C19.9539 -0.0846246 20.4083 -0.00591126 20.7071 0.292922C20.7071 0.292922 20.7071 0.292922 20.7071 0.292922ZM3.95337 9.70759C3.95337 9.70759 9.24256 11.0299 9.24256 11.0299C9.60085 11.1195 9.8806 11.3992 9.97017 11.7575C9.97017 11.7575 11.2925 17.0467 11.2925 17.0467C11.2925 17.0467 17.8976 3.10241 17.8976 3.10241C17.8976 3.10241 3.95337 9.70759 3.95337 9.70759C3.95337 9.70759 3.95337 9.70759 3.95337 9.70759Z"
                fill="#0256FF" />
            </svg>
            <span>分享</span>
          </div> -->
        </div>
      </div>

      <!-- 学习路径内容 -->
      <div class="map-content" v-if="!loading">
        <!-- 学习路径时间轴 -->
        <div class="timeline">
          <!-- 开始学习节点 -->
          <div class="timeline-node start-node">
            <div class="timeline-icon">
              <svg width="21" height="24" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg">
                <g class="layer">
                  <title>Layer 1</title>
                  <path
                    d="m17.28571,22.5q0,0.6562 0.3817,1.0781q0.38169,0.4219 0.97544,0.4219q0.59375,0 0.97544,-0.4219q0.3817,-0.4219 0.3817,-1.0781l0,-1.5l0,-14.25l0,-5.25q0,-0.65625 -0.3817,-1.07812q-0.38169,-0.42188 -0.97544,-0.42188q-0.59375,0 -0.97544,0.42188q-0.3817,0.42187 -0.3817,1.07812l0,6l-2.71429,0.75q-2.71429,0.70312 -5.2165,-0.60938q-2.92636,-1.59374 -6.02237,-0.375l-1.44192,0.60938q-0.84821,0.42188 -0.89065,1.40625l0,11.62495q0.04243,0.8438 0.63614,1.2188q0.55136,0.4219 1.27237,0.0938l0.38163,-0.2344q1.52688,-0.7969 3.13844,-0.7969q1.61156,0 3.096,0.7969q2.33257,1.2187 4.83479,0.5625l2.92634,-0.7969l0,0.75z"
                    fill="#E5E7EB" id="svg_1" transform="rotate(180 10.5 12)" />
                </g>
              </svg>
            </div>
            <div class="timeline-label">开始学习</div>
          </div>

          <!-- 学习阶段节点 -->
          <div class="timeline-line">
            <div class="timeline-line-inner" :style="{ height: timelineProgressHeight + 'px' }"></div>
            <div class="timeline-line-remaining"></div>
          </div>

          <div v-for="(stage, index) in mapDetail.stages" :key="stage.id" class="timeline-stage"
            :class="{ 'completed': stage.status === 'completed', 'current': stage.status === 'learning', 'locked': stage.status === 'locked' }"
            :style="{ top: 100 + index * 170 + 'px' }">
            <div class="timeline-node">
              <div class="timeline-circle" :class="{ 'locked': stage.status === 'locked' }">
                <template v-if="stage.status === 'locked'">
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                      d="M6.25 1.25C5.58696 1.25 4.95108 1.51339 4.48223 1.98223C4.01339 2.45108 3.75 3.08696 3.75 3.75C3.75 3.75 3.75 5.625 3.75 5.625C3.75 5.625 8.75 5.625 8.75 5.625C8.75 5.625 8.75 3.75 8.75 3.75C8.75 3.08696 8.48663 2.45108 8.01775 1.98223C7.54894 1.51339 6.91306 1.25 6.25 1.25C6.25 1.25 6.25 1.25 6.25 1.25ZM10 5.625C10 5.625 10 3.75 10 3.75C10 2.75544 9.60494 1.80161 8.90163 1.09835C8.19838 0.395088 7.24456 0 6.25 0C5.25544 0 4.30161 0.395088 3.59835 1.09835C2.89509 1.80161 2.5 2.75544 2.5 3.75C2.5 3.75 2.5 5.625 2.5 5.625C2.5 5.625 1.875 5.625 1.875 5.625C0.839469 5.625 0 6.46444 0 7.5C0 7.5 0 11.875 0 11.875C0 12.9106 0.839469 13.75 1.875 13.75C1.875 13.75 10.625 13.75 10.625 13.75C11.6606 13.75 12.5 12.9106 12.5 11.875C12.5 11.875 12.5 7.5 12.5 7.5C12.5 6.46444 11.6606 5.625 10.625 5.625C10.625 5.625 10 5.625 10 5.625C10 5.625 10 5.625 10 5.625ZM1.875 6.875C1.52983 6.875 1.25 7.15481 1.25 7.5C1.25 7.5 1.25 11.875 1.25 11.875C1.25 12.2202 1.52983 12.5 1.875 12.5C1.875 12.5 10.625 12.5 10.625 12.5C10.9702 12.5 11.25 12.2202 11.25 11.875C11.25 11.875 11.25 7.5 11.25 7.5C11.25 7.15481 10.9702 6.875 10.625 6.875C10.625 6.875 1.875 6.875 1.875 6.875C1.875 6.875 1.875 6.875 1.875 6.875Z"
                      fill="#FFFFFF" />
                  </svg>
                </template>
              </div>
            </div>
          </div>

          <!-- 完成学习节点 -->
          <div class="timeline-node end-node">
            <div class="timeline-icon">
              <svg width="27" height="24" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg">
                <g class="layer">
                  <title>Layer 1</title>
                  <path
                    d="m8.25,24l10.5,0l-10.5,0l10.5,0q0.9375,-0.0469 1.5938,-0.6562q0.6093,-0.6563 0.6093,-1.5938q0,-0.375 -0.0469,-0.75l4.9688,0q1.0312,-0.0938 1.125,-1.125q-0.0469,-3.2344 -1.0312,-5.5781q-1.0313,-2.2969 -2.6719,-3.8438q-1.5469,-1.49998 -3.2813,-2.34372q-1.7344,-0.84376 -3.1875,-1.21876q-0.7969,-0.23437 -1.3125,-0.79687q-0.5156,-0.60937 -0.5156,-1.3125q0,-0.75 0.5156,-1.26563q0.5156,-0.51562 1.2656,-0.51562l1.2188,0q0.6562,0 1.0781,-0.42188q0.4219,-0.42187 0.4219,-1.07812q0,-0.65625 -0.4219,-1.07812q-0.4219,-0.42188 -1.0781,-0.42188l-9,0q-0.65625,0 -1.07812,0.42188q-0.42188,0.42187 -0.42188,1.07812q0,0.65625 0.42188,1.07812q0.42187,0.42188 1.07812,0.42188l1.2188,0q0.75,0 1.2656,0.51562q0.5156,0.51563 0.5156,1.26563q0,0.70313 -0.5156,1.3125q-0.5156,0.5625 -1.3125,0.79687q-1.45315,0.375 -3.18752,1.21876q-1.73438,0.84374 -3.28126,2.34372q-1.64062,1.5469 -2.67187,3.8438q-0.98437,2.3437 -1.03125,5.5781q0.09375,1.0312 1.125,1.125l4.96875,0q-0.04687,0.375 -0.04687,0.75q0,0.9375 0.60937,1.5938q0.65625,0.6093 1.59375,0.6562zm16.4531,-5.25l-3.9375,0l3.9375,0l-3.9375,0q-0.3281,-3.1406 -0.9844,-5.3438q-0.6562,-2.2031 -1.4531,-3.60932q1.7813,0.75002 3.4219,2.29692q1.125,1.0781 1.9688,2.7187q0.7968,1.6406 0.9843,3.9375zm-19.4531,-6.6562q1.64062,-1.5469 3.42188,-2.29692q-0.79688,1.40622 -1.45313,3.60932q-0.65625,2.2032 -0.98437,5.3438l-3.9375,0q0.1875,-2.2969 0.98437,-3.9375q0.84375,-1.6406 1.96875,-2.7187z"
                    fill="#FFB800" id="svg_1" transform="rotate(180 13.5 12)" />
                </g>
              </svg>
            </div>
            <div class="timeline-label">完成学习</div>
          </div>
        </div>

        <!-- 学习阶段卡片 -->
        <div class="stage-cards">
          <div v-for="(stage, index) in mapDetail.stages" :key="stage.id" class="stage-card"
            :class="{ 'completed': stage.status === 'completed', 'current': stage.status === 'learning', 'locked': stage.status === 'locked' }"
            @click="goToStageDetail(stage)">
            <h3 class="stage-title">{{ stage.name }}</h3>
            <p class="stage-description">{{ getStageDescription(stage) }}</p>
            <div class="stage-progress">
              <div class="progress-bar">
                <div class="progress-bar-inner" :style="{ width: getStageProgressPercentage(stage) + '%' }"
                  :class="{ 'completed': stage.status === 'completed' }"></div>
              </div>
              <div class="progress-info">
                <span class="progress-text">{{ getStageProgressText(stage) }}</span>
                <span class="credit-text">{{ stage.credit }} 学分</span>
              </div>
            </div>
            <div class="stage-action">
              <span class="action-text">{{ getStageActionText(stage) }}</span>
            </div>
          </div>
        </div>

      </div>

      <!-- 学习人员区域 -->
      <div class="learners-section">
        <h3 class="learners-title">学习人员 <span class="learner-count">({{ mapDetail.learners ? mapDetail.learners.total :
            0
            }})</span></h3>

        <!-- 人员筛选标签 -->
        <div class="learners-tabs">
          <div class="tab-item" :class="{ active: activeLearnerTab === 'all' }" @click="activeLearnerTab = 'all'">
            全部 ({{ mapDetail.learners ? mapDetail.learners.total : 0 }})
          </div>
          <div class="tab-item" :class="{ active: activeLearnerTab === 'completed' }"
            @click="activeLearnerTab = 'completed'">
            已完成 ({{ mapDetail.learners ? mapDetail.learners.completed : 0 }})
          </div>
          <div class="tab-item" :class="{ active: activeLearnerTab === 'learning' }"
            @click="activeLearnerTab = 'learning'">
            进行中 ({{ mapDetail.learners ? mapDetail.learners.learning : 0 }})
          </div>
          <div class="tab-item" :class="{ active: activeLearnerTab === 'not_started' }"
            @click="activeLearnerTab = 'not_started'">
            未开始 ({{ mapDetail.learners ? mapDetail.learners.notStart : 0 }})
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
    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
      <a-spin size="large" />
    </div>

    <!-- 分享弹窗 -->
    <a-modal v-model:visible="shareModalVisible" title="分享学习地图" :footer="null" @cancel="shareModalVisible = false">
      <div class="share-modal-content">
        <div class="share-info">
          <h3>{{ shareInfo.title }}</h3>
          <p>{{ shareInfo.description }}</p>
        </div>
        <div class="share-qrcode">
          <img :src="shareInfo.qrCodeUrl" alt="分享二维码" />
          <p>扫码分享给好友</p>
        </div>
        <div class="share-link">
          <p>分享链接：</p>
          <div class="link-input">
            <a-input :value="shareInfo.shareUrl" readonly />
            <a-button type="primary" @click="copyShareLink">复制链接</a-button>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 页脚 -->
    <FooterComponent />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import { getMapDetail } from '@/api/toc/map';
import { getShareContent } from '@/api/toc/share';
import { UserOutlined } from '@ant-design/icons-vue';
import { recordLearningProgress } from '@/api/toc/learning';

export default defineComponent({
  name: 'MapDetailPage',
  components: {
    HeaderComponent,
    FooterComponent,
    UserOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态
    const loading = ref(true);
    const activeLearnerTab = ref('all');
    const mapDetail = ref({
      id: 0,
      name: '',
      cover: '',
      introduction: '',
      requiredCredit: 0,
      electiveCredit: 0,
      unlockMode: 0,
      theme: '',
      stages: [],
      userProgress: {
        status: 'learning',
        progress: 0,
        studyDuration: 0,
        currentStageId: null,
        completedStageCount: 0,
        earnedCredit: 0
      }
    });
    const shareModalVisible = ref(false);
    const shareInfo = ref({
      title: '',
      description: '',
      coverImage: '',
      shareUrl: '',
      qrCodeUrl: ''
    });

    // 计算属性
    const progressPercentage = computed(() => {
      return mapDetail.value.userProgress?.progress || 0;
    });

    const totalStudyTime = computed(() => {
      return mapDetail.value.userProgress?.studyDuration || 0;
    });

    const timelineProgressHeight = computed(() => {
      const totalStages = mapDetail.value.stages.length;
      if (totalStages === 0) return 0;

      const completedStages = mapDetail.value.userProgress?.completedStageCount || 0;
      const currentStage = mapDetail.value.stages.findIndex(stage =>
        stage.id === mapDetail.value.userProgress?.currentStageId
      );

      return (completedStages + 1) * 187.5;
    });

    // 根据当前选中的标签筛选学习人员
    const filteredLearners = computed(() => {
      if (!mapDetail.value.learners) return [];

      if (activeLearnerTab.value === 'all') {
        // 合并所有人员列表
        const allLearners = [];
        if (mapDetail.value.learners.completedList) {
          allLearners.push(...mapDetail.value.learners.completedList);
        }
        if (mapDetail.value.learners.learningList) {
          allLearners.push(...mapDetail.value.learners.learningList);
        }
        if (mapDetail.value.learners.notStartList) {
          allLearners.push(...mapDetail.value.learners.notStartList);
        }
        return allLearners;
      } else if (activeLearnerTab.value === 'completed') {
        return mapDetail.value.learners.completedList || [];
      } else if (activeLearnerTab.value === 'learning') {
        return mapDetail.value.learners.learningList || [];
      } else if (activeLearnerTab.value === 'not_started') {
        return mapDetail.value.learners.notStartList || [];
      }

      return [];
    });

    // 方法
    const fetchMapDetail = async () => {
      loading.value = true;
      try {
        const id = route.params.id;
        if (!id) {
          message.error('学习地图ID不能为空');
          router.push('/toc/learning/center');
          return;
        }

        const response = await getMapDetail(id);
        if (response.code === 200) {
          mapDetail.value = response.data;
        } else {
          message.error(response.message || '获取学习地图详情失败');
        }
        recordMapProgress(id);
      } catch (error) {
        console.error('获取学习地图详情出错:', error);
      } finally {
        loading.value = false;
      }
    };

    const showShareModal = async () => {
      try {
        const response = await getShareContent('map', mapDetail.value.id);
        if (response.code === 200) {
          shareInfo.value = response.data;
          shareModalVisible.value = true;
        } else {
          message.error(response.message || '获取分享信息失败');
        }
      } catch (error) {
        console.error('获取分享信息出错:', error);
      }
    };

    const copyShareLink = () => {
      const input = document.createElement('input');
      input.value = shareInfo.value.shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      message.success('链接已复制到剪贴板');
    };

    const formatStudyTime = (minutes) => {
      if (!minutes) return '0分钟';

      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;

      if (hours > 0) {
        return `${hours}小时${mins > 0 ? mins + '分钟' : ''}`;
      }
      return `${mins}分钟`;
    };

    const getStageDescription = (stage) => {
      // 这里可以根据实际情况从stage中获取描述
      // 如果stage中没有描述字段，可以返回一个默认描述
      return stage.description || '完成本阶段学习任务，掌握相关知识和技能';
    };

    const getStageProgressPercentage = (stage) => {
      if (stage.status === 'completed') return 100;
      if (stage.status === 'locked') return 0;

      // 如果有具体进度，返回具体进度
      if (stage.progress !== undefined) return stage.progress;

      // 默认进度
      return 0;
    };

    const getStageProgressText = (stage) => {
      if (stage.status === 'completed') return '进度 100%';
      if (stage.status === 'locked') return '未开始';

      // 如果有具体进度，返回具体进度
      if (stage.progress !== undefined) return `进度 ${stage.progress}%`;

      // 默认进度文本
      return '进度 0%';
    };

    const getStageActionText = (stage) => {
      if (stage.status === 'locked') return '未解锁';
      return '去学习 →';
    };

    const goToStageDetail = (stage) => {
      if (stage.status === 'locked') {
        message.info('该阶段尚未解锁，请先完成前面的学习阶段');
        return;
      }

      router.push({
        path: '/toc/map/stage/detail',
        query: {
          mapId: mapDetail.value.id,
          stageId: stage.id
        }
      });
    };

    const recordMapProgress = async (id) => {
      try {
        const request = {
          "contentType": 'LEARNING_MAP',
          "parentType": '',  // COURSE-课程/SERIES-系列课程/LEARNING_MAP-学习地图/MAP_STAGE-地图阶段/TRAIN-培训
          "contentId": id, // 当前节点id, 例如培训场景，这里传课程id
          "parentId": '', // 父节点id，例如培训场景，这里传培训id
          "progress": 0, // 进度
          "duration": 0 // 学习时长 秒数
        };

        await recordLearningProgress(request);
      } catch (error) {
        console.error('记录学习进度出错:', error);
      }
    };

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

    // 生命周期钩子
    onMounted(() => {
      fetchMapDetail();
    });

    return {
      loading,
      mapDetail,
      progressPercentage,
      totalStudyTime,
      timelineProgressHeight,
      shareModalVisible,
      shareInfo,
      activeLearnerTab,
      filteredLearners,
      formatStudyTime,
      getStageDescription,
      getStageProgressPercentage,
      getStageProgressText,
      getStageActionText,
      getLearnerStatusColor,
      getLearnerStatusTagColor,
      getLearnerStatusText,
      goToStageDetail,
      showShareModal,
      copyShareLink
    };
  }
});
</script>

<style scoped>
.map-detail-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 24px 32px;
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
}

.map-header {
  padding: 24px 32px;
}

.map-title {
  font-size: 24px;
  font-weight: bold;
  color: #000000;
  margin-bottom: 16px;
}

.map-progress-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-bar-container {
  display: flex;
  align-items: center;
}

.progress-bar {
  width: 192px;
  height: 8px;
  background-color: #E5E7EB;
  border-radius: 9999px;
  overflow: hidden;
  margin-right: 16px;
}

.progress-bar-inner {
  height: 100%;
  background-color: #4B83EE;
  border-radius: 9999px;
}

.progress-text {
  font-size: 14px;
  color: #6B7280;
  margin-right: 16px;
}

.study-time {
  font-size: 14px;
  color: #6B7280;
}

.share-button {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.share-button svg {
  margin-right: 5px;
}

.share-button span {
  font-size: 20px;
  color: #2563EB;
}

.map-content {
  display: flex;
  position: relative;
  padding: 0 32px;
  /* min-height: 800px; */
  overflow: hidden;
  padding-bottom: 100px;
}

.timeline {
  position: relative;
  width: 60px;
  margin-right: 30px;
}

.timeline-line {
  position: absolute;
  left: 26px;
  top: 106px;
  width: 8px;
  height: calc(100% - 90px);
  display: flex;
  flex-direction: column;
}

.timeline-line-inner {
  width: 100%;
  background: linear-gradient(180deg, #4B83EE 0%, #6BA2FF 100%);
}

.timeline-line-remaining {
  flex-grow: 1;
  background-color: #E5E7EB;
}

.timeline-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-icon {
  width: 53px;
  height: 64px;
  background-color: #FFFFFF;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0px 2px 4px -2px rgba(0, 0, 0, 0.1), 0px 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.timeline-label {
  margin-top: 8px;
  font-size: 14px;
  color: #4B5563;
  text-align: center;
}

.start-node {
  position: absolute;
  top: 0;
}

.end-node {
  position: absolute;
  bottom: -100px;
}

.timeline-stage {
  position: absolute;
  left: 18px;
}

.timeline-circle {
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background-color: #4B83EE;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.timeline-circle::before {
  content: '';
  position: absolute;
  width: 36px;
  height: 36px;
  border-radius: 18px;
  border: 2px dashed #4B83EE;
  box-sizing: border-box;
}

.timeline-circle.locked {
  background-color: #E5E7EB;
}

.timeline-circle.locked::before {
  border-color: #E5E7EB;
}

.stage-cards {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 40px;
}

.stage-card {
  width: 300px;
  padding: 16px;
  background-color: #FFFFFF;
  border-radius: 8px;
  box-shadow: 0px 4px 12px 0px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s;
}

.stage-card:hover {
  transform: translateY(-2px);
}

.stage-card.locked {
  opacity: 0.8;
  cursor: not-allowed;
}

.stage-title {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  margin-bottom: 8px;
}

.stage-description {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 14px;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.stage-progress {
  margin-bottom: 8px;
}

.stage-progress .progress-bar {
  width: 100%;
  margin-bottom: 6px;
}

.progress-bar-inner.completed {
  background-color: #1890FF;
}

.progress-info {
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

.stage-action {
  margin-top: 8px;
}

.action-text {
  font-size: 14px;
  color: #4B83EE;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.share-modal-content {
  padding: 16px;
}

.share-info {
  margin-bottom: 24px;
}

.share-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.share-info p {
  font-size: 14px;
  color: #6B7280;
}

.share-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.share-qrcode img {
  width: 200px;
  height: 200px;
  margin-bottom: 8px;
}

.share-qrcode p {
  font-size: 14px;
  color: #6B7280;
}

.share-link p {
  font-size: 14px;
  margin-bottom: 8px;
}

.link-input {
  display: flex;
  gap: 8px;
}

.learners-section {
  margin-top: 40px;
  border-top: 1px solid #e5e7eb;
  padding-top: 24px;
  background-color: #FFFFFF;
  border-radius: 8px;
  padding: 24px;
  margin-top: 24px;
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

.learners-tabs .tab-item {
  padding: 8px 0;
  margin-right: 32px;
  font-size: 16px;
  color: #6B7280;
  cursor: pointer;
  position: relative;
}

.learners-tabs .tab-item.active {
  color: #1890FF;
  font-weight: 600;
}

.learners-tabs .tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #3B82F6;
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
</style>
