<template>
  <div class="map-detail-mobile-page">
    <div class="main-content">
      <!-- 学习地图进度 -->
      <div class="map-progress-container">
        <div class="progress-bar-container">
          <div class="progress-bar">
            <div class="progress-bar-inner" :style="{ width: progressPercentage + '%' }"></div>
          </div>
          <div class="progress-info">
            <span class="progress-text">学习进度 {{ progressPercentage }}%</span>
            <span class="study-time">预计学习时间：{{ formatStudyTime(totalStudyTime) }}</span>
          </div>
        </div>
        <!-- <div class="share-button" @click="showShareModal">
          <share-alt-outlined />
          <span>分享</span>
        </div> -->
      </div>

      <!-- 学习路径内容 -->
      <div class="map-content" v-if="!loading">
        <!-- 学习阶段卡片 -->
        <div class="stage-cards">
          <div
            v-for="(stage, index) in mapDetail.stages"
            :key="stage.id"
            class="stage-card"
            :class="{
              'completed': stage.status === 'completed',
              'current': stage.status === 'learning',
              'locked': stage.status === 'locked'
            }"
            @click="goToStageDetail(stage)"
          >
            <div class="stage-status-icon">
              <check-outlined v-if="stage.status === 'completed'" />
              <div v-else-if="stage.status === 'learning'" class="current-dot"></div>
              <lock-outlined v-else />
            </div>
            <h3 class="stage-title">{{ stage.name }}</h3>
            <div class="stage-progress">
              <div class="progress-bar">
                <div
                  class="progress-bar-inner"
                  :style="{ width: getStageProgressPercentage(stage) + '%' }"
                  :class="{ 'completed': stage.status === 'completed' }"
                ></div>
              </div>
              <div class="progress-info">
                <span class="progress-text">{{ getStageProgressText(stage) }}</span>
                <span class="credit-text">{{ stage.completedHours || 0 }}/{{ stage.totalHours || 0 }} 课时</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <a-spin size="large" />
      </div>
    </div>

    <!-- 分享弹窗 -->
    <a-drawer
      v-model:visible="shareModalVisible"
      title="分享学习地图"
      placement="bottom"
      height="70vh"
      :destroyOnClose="true"
      @close="shareModalVisible = false"
    >
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
            <a-input-group compact>
              <a-input
                :value="shareInfo.shareUrl"
                readonly
                style="width: calc(100% - 80px)"
              />
              <a-button type="primary" @click="copyShareLink">复制链接</a-button>
            </a-input-group>
          </div>
        </div>
      </div>
    </a-drawer>

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import { getMapDetail } from '@/api/toc/map';
import { getShareContent } from '@/api/toc/share';
import {
  ShareAltOutlined,
  FlagOutlined,
  TrophyOutlined,
  LockOutlined,
  CheckOutlined,
  RightOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'MapDetailMobilePage',
  components: {
    MobileTabBar,
    ShareAltOutlined,
    FlagOutlined,
    TrophyOutlined,
    LockOutlined,
    CheckOutlined,
    RightOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态
    const loading = ref(true);
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

    // 方法
    const fetchMapDetail = async () => {
      const mapId = route.params.id;
      if (!mapId) {
        message.error('学习地图ID不能为空');
        router.push('/toc/mobile/learning/center');
        return;
      }

      loading.value = true;
      try {
        const res = await getMapDetail(mapId);
        if (res.code === 200 && res.data) {
          mapDetail.value = res.data;

          // 处理课时数据
          mapDetail.value.stages.forEach(stage => {
            // 如果后端没有提供课时数据，设置默认值
            if (!stage.completedHours) {
              if (stage.status === 'completed') {
                stage.completedHours = stage.totalHours || 8;
              } else if (stage.status === 'learning') {
                // 根据进度计算已完成课时
                stage.completedHours = Math.floor(((stage.progress || 0) / 100) * (stage.totalHours || 8));
              } else {
                stage.completedHours = 0;
              }
            }

            // 确保有总课时
            if (!stage.totalHours) {
              stage.totalHours = 8;
            }
          });
        } else {
          message.error(res.message || '获取学习地图详情失败');
        }
      } catch (error) {
        console.error('获取学习地图详情失败:', error);
      } finally {
        loading.value = false;
      }
    };

    const formatStudyTime = (minutes) => {
      if (!minutes || minutes <= 0) return '0分钟';

      if (minutes < 60) {
        return `${minutes}分钟`;
      } else {
        const hours = Math.floor(minutes / 60);
        const remainingMinutes = minutes % 60;
        return remainingMinutes > 0 ? `${hours}小时${remainingMinutes}分钟` : `${hours}小时`;
      }
    };

    const getStageProgressPercentage = (stage) => {
      if (stage.status === 'completed') return 100;
      if (stage.status === 'locked') return 0;
      return stage.progress || 0;
    };

    const getStageProgressText = (stage) => {
      if (stage.status === 'completed') return '已完成';
      if (stage.status === 'locked') return '未开始';
      return `进行中 ${stage.progress || 0}%`;
    };

    const goToStageDetail = (stage) => {
      if (stage.status === 'locked') {
        message.warning('该阶段尚未解锁，请先完成前置阶段');
        return;
      }

      router.push(`/toc/mobile/map/stage?mapId=${mapDetail.value.id}&stageId=${stage.id}`);
    };

    const showShareModal = async () => {
      shareModalVisible.value = true;

      try {
        const res = await getShareContent('learning_map', mapDetail.value.id);
        if (res.code === 200 && res.data) {
          shareInfo.value = res.data;
        } else {
          message.error(res.message || '获取分享信息失败');
        }
      } catch (error) {
        console.error('获取分享信息失败:', error);
      }
    };

    const copyShareLink = () => {
      if (!shareInfo.value.shareUrl) {
        message.error('分享链接不存在');
        return;
      }

      const input = document.createElement('input');
      input.value = shareInfo.value.shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);

      message.success('分享链接已复制到剪贴板');
    };

    onMounted(() => {
      fetchMapDetail();
    });

    return {
      loading,
      mapDetail,
      shareModalVisible,
      shareInfo,
      progressPercentage,
      totalStudyTime,
      formatStudyTime,
      getStageProgressPercentage,
      getStageProgressText,
      goToStageDetail,
      showShareModal,
      copyShareLink
    };
  }
});
</script>

<style scoped>
.map-detail-mobile-page {
  padding-bottom: 65px;
  background-color: #f5f5f5;
}

.main-content {
  padding: 16px;
  position: relative;
}

.map-progress-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-bar-container {
  flex: 1;
  margin-right: 16px;
}

.progress-bar {
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar-inner {
  height: 100%;
  background-color: #1890ff;
  border-radius: 4px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

.share-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #1890ff;
  font-size: 12px;
  cursor: pointer;
}

.share-button .anticon {
  font-size: 20px;
  margin-bottom: 4px;
}

.map-content {
  position: relative;
}

.stage-cards {
  width: 100%;
}

.stage-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  position: relative;
}

.stage-card.locked {
  opacity: 0.8;
  background-color: #f9f9f9;
}

.stage-card.completed {
  border-left: 3px solid #52c41a;
}

.stage-card.current {
  border-left: 3px solid #5b5cff;
}

.stage-status-icon {
  position: absolute;
  left: -10px;
  top: 16px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stage-status-icon .anticon {
  color: #52c41a;
  font-size: 14px;
}

.stage-status-icon .current-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #5b5cff;
}

.stage-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
}

.stage-progress {
  margin-bottom: 8px;
}

.stage-progress .progress-bar {
  height: 6px;
  margin-bottom: 8px;
}

.stage-progress .progress-bar-inner {
  background-color: #5b5cff;
}

.stage-progress .progress-bar-inner.completed {
  background-color: #52c41a;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

.loading-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.7);
}

.share-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-info h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.share-info p {
  color: #666;
  font-size: 14px;
}

.share-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 16px 0;
}

.share-qrcode img {
  width: 180px;
  height: 180px;
  margin-bottom: 8px;
}

.share-qrcode p {
  color: #666;
  font-size: 14px;
}

.share-link p {
  margin-bottom: 8px;
}
</style>