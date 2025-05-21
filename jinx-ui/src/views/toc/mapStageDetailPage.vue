<template>
  <div class="map-stage-detail-page">
    <!-- 顶部导航 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <div class="main-content">
      <!-- 阶段内容 -->
      <div class="stage-content" v-if="!loading">
        <!-- 阶段标题和开始学习按钮 -->
        <div class="stage-header">
          <div class="stage-title-container">
            <div class="stage-status" :class="{ 'completed': isStageCompleted }">
              <span v-if="isStageCompleted">学习完成</span>
              <span v-else>{{ stageDetail.name }}</span>
            </div>
            <div class="stage-score" v-if="!isStageCompleted">
              <a-rate :value="0" :count="1" disabled />
              <span>0学分</span>
            </div>
          </div>
        </div>
        <!-- 学习路线图 -->
        <div class="learning-road-container">
          <div class="road-bg">
            <img src="@/assets/images/road-head.svg" alt="road-head" class="road-head">
            <img src="@/assets/images/road-line.svg" v-for="i in stageDetail.tasks.length" :key="i" alt="road-line" class="road-line">
          </div>

          <!-- 起点标记 -->
          <div class="road-start">
            <div class="start-circle">
              <span>Start</span>
            </div>
            <div class="start-cone"></div>
          </div>

          <div class="task-node-container">
            <!-- 任务节点 -->
            <div v-for="(task, index) in stageDetail.tasks" :key="task.id" class="task-node-item"
              :class="{ 'left-side': index % 2 === 0, 'right-side': index % 2 !== 0 }">

              <!-- 公路上的节点标记 -->
              <div class="road-node"
                :class="{ 'completed': task.status === 'completed', 'learning': task.status === 'learning' }"></div>

              <!-- 任务卡片 -->
              <div class="task-card"
                :class="{ 'completed': task.status === 'completed', 'learning': task.status === 'learning', 'locked': task.status === 'locked', 'left-card': index % 2 === 0, 'right-card': index % 2 !== 0 }">
                <div class="task-header">
                  <div class="task-tag" v-if="task.isRequired === 1">必学</div>
                  <div class="task-type-tag">「{{ getContentTypeText(task.contentType || task.type) }}」</div>
                  <h3 class="task-title">{{ task.title }}</h3>
                </div>
                <div class="task-action">
                  <span class="action-text" @click="goToTask(task)">{{ getActionText(task) }}</span>
                </div>
                <!-- 三角指针 -->
                <div class="card-pointer"></div>
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

    <!-- 空状态 -->
    <EmptyState v-if="!loading && (!stageDetail.tasks || stageDetail.tasks.length === 0)" title="暂无学习任务"
      description="该阶段暂未添加学习任务，请稍后再来查看">
      <template #action>
        <a-button type="primary" @click="goToMapDetail">返回学习地图</a-button>
      </template>
    </EmptyState>

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
import EmptyState from '@/components/common/EmptyState.vue';
import { getMapStageDetail } from '@/api/toc/map';
import { recordLearningProgress } from '@/api/toc/learning';

export default defineComponent({
  name: 'MapStageDetailPage',
  components: {
    HeaderComponent,
    FooterComponent,
    EmptyState
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态
    const loading = ref(true);
    const mapId = ref(null);
    const stageId = ref(null);
    const mapName = ref('学习地图');
    const stageDetail = ref({
      id: 0,
      mapId: 0,
      name: '',
      stageOrder: 0,
      credit: 0,
      openType: 0,
      status: '',
      userProgress: {
        completedTaskCount: 0,
        totalTaskCount: 0,
        progress: 0
      },
      tasks: []
    });

    // 计算属性
    const isStageCompleted = computed(() => {
      return stageDetail.value.status === 'completed';
    });

    const hasStartedLearning = computed(() => {
      return stageDetail.value.status === 'learning' ||
        (stageDetail.value.userProgress &&
          stageDetail.value.userProgress.progress > 0);
    });

    // 方法
    const fetchStageDetail = async () => {
      loading.value = true;
      try {
        mapId.value = route.query.mapId;
        stageId.value = route.query.stageId;

        if (!mapId.value || !stageId.value) {
          message.error('参数错误：缺少学习地图ID或阶段ID');
          router.push('/toc/learning/center');
          return;
        }

        const response = await getMapStageDetail(mapId.value, stageId.value);
        if (response.code === 200) {
          stageDetail.value = response.data;

          // 获取地图名称
          if (sessionStorage.getItem('mapName_' + mapId.value)) {
            mapName.value = sessionStorage.getItem('mapName_' + mapId.value);
          }
        } else {
          message.error(response.message || '获取阶段详情失败');
        }
      // 记录学习进度
      recordTaskProgress(response.data.id);
      } catch (error) {
        console.error('获取阶段详情出错:', error);
      } finally {
        loading.value = false;
      }
    };

    const getContentTypeText = (contentType) => {
      if (!contentType) return '未知';

      switch (contentType.toLowerCase()) {
        case 'video':
          return '视频';
        case 'document':
          return '文档';
        case 'article':
          return '文章';
        case 'exam':
          return '考试';
        case 'assignment':
          return '作业';
        case 'survey':
          return '调研';
        case 'train':
          return '培训';
        case 'course':
          return '课程';
        default:
          return contentType;
      }
    };

    const getProgressText = (task) => {
      if (task.status === 'completed') return '已完成';
      if (task.status === 'learning') return `进度：${task.progress}%`;
      if (task.status === 'locked') return '未开始';
      return '未开始';
    };

    const getActionText = (task) => {
      if (task.status === 'locked') return '待解锁 →';
      return '去学习 →';
    };

    const goToPersonalCenter = () => {
      router.push('/toc/personal/center');
    };

    const goToMapDetail = () => {
      router.push({
        path: '/toc/map/detail',
        query: { id: mapId.value }
      });
    };

    const goToTask = (task) => {
      if (task.status === 'locked') {
        message.info('该任务尚未解锁，请先完成前面的任务');
        return;
      }

      let path = '';
      if (task.type === 'TRAIN') {
        path = '/toc/train/detail/' + task.id;
      } else if (task.type === 'COURSE') {
        path = '/toc/course/detail/' + task.id;
      } else {
        path = '/toc/course/detail/' + task.id;
      }

      router.push({ path });

    };

    const recordTaskProgress = async (id) => {
      try {
        const request = {
          "contentType": 'MAP_STAGE',
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

    // 生命周期钩子
    onMounted(() => {
      fetchStageDetail();
    });

    return {
      loading,
      mapId,
      stageId,
      mapName,
      stageDetail,
      isStageCompleted,
      hasStartedLearning,
      getContentTypeText,
      getProgressText,
      getActionText,
      goToPersonalCenter,
      goToMapDetail,
      goToTask,
    };
  }
});
</script>

<style scoped>
.map-stage-detail-page {
  min-height: 100vh;
  background-color: #F5F7FA;
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

.stage-content {
  flex: 1;
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  position: relative;
}

.stage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.stage-title-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stage-status {
  display: inline-flex;
  padding: 8px 16px;
  background-color: #4B83EE;
  color: #FFFFFF;
  border-radius: 9999px;
  font-size: 16px;
  font-weight: 400;
}

.stage-status.completed {
  background-color: #22C55E;
}

.stage-score {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #F59E0B;
  font-size: 14px;
}

/* 学习路线图样式 */
.learning-road-container {
  position: relative;
  min-height: 600px;
  margin-top: 40px;
  padding-bottom: 100px;
}

/* 公路背景 */
.road-bg {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.road-bg .road-head {
  margin-left: -140px;
}
.road-bg .road-line {
  margin-left: 40px;
}

/* 起点样式 */
.road-start {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: -60px;
}

.start-circle {
  width: 60px;
  height: 60px;
  background-color: #4B83EE;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.start-cone {
  width: 0;
  height: 0;
  border-left: 15px solid transparent;
  border-right: 15px solid transparent;
  border-top: 20px solid #F59E0B;
  margin-top: 5px;
}

/* 终点样式 */
.road-end {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.end-circle {
  width: 30px;
  height: 30px;
  background-color: #22C55E;
  border-radius: 50%;
  border: 5px solid white;
}

/* 任务节点容器 */
.task-node-container {
  position: absolute;
  z-index: 5;
  top: 200px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 60px;
}

.task-node-item {
  position: relative;
  width: 280px;
}

/* 左侧节点 */
.left-side {
  margin-left: -300px;
}

/* 右侧节点 */
.right-side {
  margin-left: 380px;
}

.left-side .road-node {
  top: 52px;
  right: -41px;
}

.right-side .road-node {
  top: 52px;
  left: -43px;
}

/* 公路上的节点标记 */
.road-node {
  position: absolute;
  width: 24px;
  height: 24px;
  background-color: white;
  border: 5px solid #E5E7EB;
  border-radius: 50%;
  z-index: 15;
}

.road-node.completed,
.road-node.learning {
  background-color: #4B83EE;
  border-color: white;
}

/* 任务卡片 */
.task-card {
  width: 100%;
  padding: 16px;
  background-color: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #E5E7EB;
  position: relative;
}

/* 卡片三角指针 */
.card-pointer {
  position: absolute;
  width: 0;
  height: 0;
  top: 50%;
  transform: translateY(-50%);
}

.left-card .card-pointer {
  right: -10px;
  border-top: 10px solid transparent;
  border-bottom: 10px solid transparent;
  border-left: 10px solid #FFFFFF;
}

.right-card .card-pointer {
  left: -10px;
  border-top: 10px solid transparent;
  border-bottom: 10px solid transparent;
  border-right: 10px solid #FFFFFF;
}

/* 已完成和学习中的卡片指针颜色 */
.task-card.completed.left-card .card-pointer {
  border-left-color: #4B83EE;
}

.task-card.completed.right-card .card-pointer {
  border-right-color: #4B83EE;
}

.task-card.learning.left-card .card-pointer {
  border-left-color: #4B83EE;
}

.task-card.learning.right-card .card-pointer {
  border-right-color: #4B83EE;
}

/* 锁定卡片指针颜色 */
.task-card.locked.left-card .card-pointer {
  border-left-color: #4B83EE;
}

.task-card.locked.right-card .card-pointer {
  border-right-color: #4B83EE;
}

.task-card.completed {
  border-color: #4B83EE;
}

.task-card.learning {
  border-color: #4B83EE;
}

.task-card.locked {
  border-color: #9CA3AF;
  background-color: #F3F4F6;
}

.task-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.task-tag {
  display: inline-flex;
  padding: 4px 8px;
  background-color: #EFF6FF;
  color: #4B83EE;
  border-radius: 4px;
  font-size: 14px;
}

.task-type-tag {
  font-size: 14px;
  color: #6B7280;
}

.task-title {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  margin: 8px 0 0 0;
  width: 100%;
}

.task-action {
  margin-top: 16px;
  text-align: right;
}

.action-text {
  font-size: 14px;
  color: #4B83EE;
  cursor: pointer;
}

.task-card.locked .action-text {
  color: #9CA3AF;
}

.bottom-action {
  display: flex;
  justify-content: center;
  padding: 24px 0;
  background-color: #F5F7FA;
}

.action-button {
  width: 145px;
  height: 40px;
  border-radius: 9999px;
  font-size: 16px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

@media (max-width: 768px) {
  .stage-content {
    padding: 16px;
  }

  .task-node-container {
    width: 100%;
    position: relative;
    margin-bottom: 30px;
    transform: none !important;
    left: auto !important;
    right: auto !important;
    top: auto !important;
  }

  .road-svg {
    display: none;
  }

  .road-start,
  .road-end {
    position: relative;
    left: 50%;
    transform: translateX(-50%);
    margin: 20px 0;
    top: auto !important;
  }

  .road-node {
    display: none;
  }

  .card-pointer {
    display: none;
  }
}
</style>
