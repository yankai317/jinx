<template>
  <div class="map-stage-detail-mobile-page">
    <div class="main-content">
      <!-- 阶段内容 -->
      <div class="stage-content" v-if="!loading">
        <!-- 阶段进度 -->
        <div class="stage-progress-container">
          <div class="progress-info">
            <div class="progress-text">
              <span>学习进度</span>
              <span>{{ stageDetail.userProgress?.progress || 0 }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-bar-inner" :style="{ width: (stageDetail.userProgress?.progress || 0) + '%' }"
                :class="{ 'completed': isStageCompleted }"></div>
            </div>
            <div class="task-count">
              已完成 {{ stageDetail.userProgress?.completedTaskCount || 0 }}/{{ stageDetail.userProgress?.totalTaskCount ||
              0 }} 个任务
            </div>
          </div>
          <div class="stage-status" :class="{ 'completed': isStageCompleted }">
            <trophy-outlined v-if="isStageCompleted" />
            {{ isStageCompleted ? '已完成' : '进行中' }}
          </div>
        </div>

        <!-- 学习路线 -->
        <div class="learning-path">
          <div class="path-title">学习路线</div>

          <!-- 任务列表 -->
          <div class="task-list">
            <div v-for="(task, index) in stageDetail.tasks" :key="task.id" class="task-item" :class="{
              'completed': task.status === 'completed',
              'learning': task.status === 'learning',
              'locked': task.status === 'locked'
            }" @click="goToTask(task)">
              <!-- 任务序号 -->
              <div class="task-index">
                <div class="index-circle">{{ index + 1 }}</div>
                <div class="index-line" v-if="index < stageDetail.tasks.length - 1"></div>
              </div>

              <!-- 任务内容 -->
              <div class="task-content">
                <div class="task-header">
                  <div class="task-tags">
                    <div class="task-tag" v-if="task.isRequired === 1">必学</div>
                    <div class="task-type-tag">{{ getContentTypeText(task.contentType || task.type) }}</div>
                  </div>
                  <h3 class="task-title">{{ task.title }}</h3>
                </div>

                <div class="task-footer">
                  <div class="task-progress" v-if="task.status !== 'locked'">
                    <div class="progress-bar">
                      <div class="progress-bar-inner" :style="{ width: (task.progress || 0) + '%' }"
                        :class="{ 'completed': task.status === 'completed' }"></div>
                    </div>
                    <span class="progress-text">{{ getProgressText(task) }}</span>
                  </div>
                  <div class="task-action">
                    <span class="action-text">{{ getActionText(task) }}</span>
                    <right-outlined />
                  </div>
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
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getMapStageDetail } from '@/api/toc/map';
import { recordLearningProgress } from '@/api/toc/learning';
import {
  TrophyOutlined,
  RightOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'MapStageDetailMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    TrophyOutlined,
    RightOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态
    const loading = ref(true);
    const mapId = ref(null);
    const stageId = ref(null);
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

    // 方法
    const fetchStageDetail = async () => {
      mapId.value = route.query.mapId;
      stageId.value = route.query.stageId;

      if (!mapId.value || !stageId.value) {
        message.error('参数错误：缺少学习地图ID或阶段ID');
        router.push('/toc/mobile/learning/center');
        return;
      }

      loading.value = true;
      try {
        const response = await getMapStageDetail(mapId.value, stageId.value);
        if (response.code === 200 && response.data) {
          stageDetail.value = response.data;
          mapId.value = response.data.mapId;

          // 记录学习进度
          recordTaskProgress(stageId.value);
        } else {
          message.error(response.message || '获取阶段详情失败');
        }
      } catch (error) {
        console.error('获取阶段详情出错:', error);
      } finally {
        loading.value = false;
      }
    };

    // 记录任务学习进度
    const recordTaskProgress = async (taskId) => {
      try {
        await recordLearningProgress({
          "contentType": 'MAP_STAGE',
          "parentType": '',  // COURSE-课程/SERIES-系列课程/LEARNING_MAP-学习地图/MAP_STAGE-地图阶段/TRAIN-培训
          "contentId": taskId, // 当前节点id, 例如培训场景，这里传课程id
          "parentId": '', // 父节点id，例如培训场景，这里传培训id
          "progress": 0, // 进度
          "duration": 0 // 学习时长 秒数
        });
      } catch (error) {
        console.error('记录学习进度失败:', error);
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
      if (task.status === 'learning') return `${task.progress || 0}%`;
      return '未开始';
    };

    const getActionText = (task) => {
      if (task.status === 'completed') return '查看详情';
      if (task.status === 'learning') return '继续学习';
      if (task.status === 'locked') return '未解锁';
      return '开始学习';
    };

    const goToTask = (task) => {
      if (task.status === 'locked') {
        message.warning('请先完成前置任务');
        return;
      }

      console.log(task);
      let path = '';
      if (task.type === 'TRAIN') {
        path = '/toc/mobile/train/' + task.id;
      } else if (task.type === 'COURSE') {
        path = '/toc/mobile/course/' + task.id;
      } else {
        path = '/toc/mobile/course/' + task.id;
      }

      router.push(path);
    };

    const goToMapDetail = () => {
      if (mapId.value) {
        router.push(`/toc/mobile/map/${mapId.value}`);
      } else {
        router.push('/toc/mobile/learning/center');
      }
    };

    onMounted(() => {
      fetchStageDetail();
    });

    return {
      loading,
      stageDetail,
      isStageCompleted,
      getContentTypeText,
      getProgressText,
      getActionText,
      goToTask,
      goToMapDetail
    };
  }
});
</script>

<style scoped>
.map-stage-detail-mobile-page {
  padding-bottom: 65px;
}

.main-content {
  padding: 16px;
  position: relative;
}

.stage-progress-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-info {
  flex: 1;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
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

.progress-bar-inner.completed {
  background-color: #52c41a;
}

.task-count {
  font-size: 12px;
  color: #666;
}

.stage-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #1890ff;
  margin-left: 16px;
}

.stage-status.completed {
  color: #52c41a;
}

.stage-status .anticon {
  font-size: 20px;
  margin-bottom: 4px;
}

.learning-path {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.path-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.task-list {
  display: flex;
  flex-direction: column;
}

.task-item {
  display: flex;
  margin-bottom: 16px;
}

.task-item.locked {
  opacity: 0.7;
}

.task-index {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 12px;
}

.index-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-bottom: 4px;
}

.task-item.completed .index-circle {
  background-color: #52c41a;
}

.task-item.locked .index-circle {
  background-color: #d9d9d9;
}

.index-line {
  width: 2px;
  height: 40px;
  background-color: #1890ff;
}

.task-item.completed .index-line {
  background-color: #52c41a;
}

.task-item.locked .index-line {
  background-color: #d9d9d9;
}

.task-content {
  flex: 1;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 12px;
}

.task-item.completed .task-content {
  border-left: 3px solid #52c41a;
}

.task-item.learning .task-content {
  border-left: 3px solid #1890ff;
}

.task-header {
  margin-bottom: 12px;
}

.task-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.task-tag {
  background-color: #ff4d4f;
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.task-type-tag {
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.task-title {
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  line-height: 1.4;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-progress .progress-bar {
  width: 60px;
  height: 4px;
  margin-bottom: 0;
}

.progress-text {
  font-size: 12px;
  color: #666;
}

.task-action {
  display: flex;
  align-items: center;
  color: #1890ff;
  font-size: 12px;
}

.task-item.locked .task-action {
  color: #d9d9d9;
}

.action-text {
  margin-right: 4px;
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
</style>