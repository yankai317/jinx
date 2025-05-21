<template>
  <div class="course-detail-page">
    <!-- 头部导航 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <!-- 主体内容 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 课程详情 -->
        <div class="course-detail-container">
          <!-- 课程头部信息 -->
          <div class="course-header">
            <div class="course-title">
              <h1>{{ currentCourse.title }}</h1>
            </div>
            <div class="course-meta">
              <span class="creator-info">
                <span v-if="currentCourse?.instructor?.name">
                  {{ currentCourse?.instructor?.name }} {{ formatDate(currentCourse.createdAt) }}
                </span>
              </span>
              <span class="progress-info" v-if="courseDetail.type === 'series'">
                已学 {{ currentCourse?.progress || 0 }}%
              </span>
              <span class="progress-info" v-else>
                已学 {{ currentCourse.userProgress?.progress || 0 }}%
              </span>
            </div>
          </div>

          <!-- 课程内容区 -->
          <div class="course-content" :class="{ 'with-sidebar': courseDetail.type === 'series' }">
            <!-- 系列课程侧边栏 -->
            <div v-if="courseDetail.type === 'series'" class="course-sidebar">
              <div class="sidebar-header">
                <h3>课程目录</h3>
              </div>
              <div class="sidebar-content">
                <div v-for="(item, index) in courseDetail.courseItems" :key="item.id"
                  :class="['sidebar-item', { active: currentCourse.id === item.id }]" @click="switchCourse(item)">
                  <div class="item-index">{{ index + 1 }}</div>
                  <div class="item-info">
                    <div class="item-title">{{ item.title }}</div>
                    <div class="item-type">{{ getTypeText(item.type) }}</div>
                  </div>
                  <div class="item-progress" v-if="item.status === 'completed'">
                    <CheckCircleOutlined style="font-size: 20px; color: #1890ff" />
                  </div>
                  <div class="item-progress" v-else>
                    <div class="progress-circle">
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 内容展示区 -->
            <div class="content-display">
              <div v-if="currentCourse.type === 'article'">
                <iframe :src="currentCourse.appendixPath" width="100%"
                  height="600" frameborder="0" crossorigin="anonymous"></iframe>
              </div>
              <!-- 视频播放器 -->
              <div v-if="currentCourse.type === 'video'" class="video-container">
                <video ref="videoPlayer" class="video-player" controls :src="currentCourse.appendixPath"
                  @timeupdate="handleVideoProgress">
                  <source :src="currentCourse.appendixPath" type="video/mp4">
                  您的浏览器不支持视频播放
                </video>
                <div class="video-controls">
                  <div class="speed-control">
                    <div class="current-speed">
                      {{ currentPlaybackRate }}x
                    </div>
                    <div class="speed-options">
                      <div v-for="rate in [1, 1.25, 1.5, 2]" :key="rate"
                        :class="['speed-option', { active: currentPlaybackRate === rate }]"
                        @click="setPlaybackRate(rate)">
                        {{ rate }}x
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- PDF文档查看器 -->
              <div v-else-if="currentCourse.type === 'document'" class="document-container">
                <iframe :src="currentCourse.appendixPath" class="pdf-viewer" frameborder="0"></iframe>
              </div>
            </div>
          </div>
          <!-- 学习人员 -->
          <div class="learners-section">
            <h3 class="learners-title">学习人员 <span class="learner-count">({{ courseDetail.learners ?
              courseDetail.learners.length : 0 }})</span></h3>
            <div class="learners-list"><a-empty v-if="!courseDetail.learners?.length" description="暂无学习人员" />
              <div v-else class="learners-grid">
                <div v-for="(learner, index) in courseDetail.learners" :key="index" class="learner-item"><a-tooltip
                    :title="learner.nickname + (learner.department ? ' - ' + learner.department : '')"><a-badge
                      :dot="learner.status === 'completed'"
                      :color="learner.status === 'completed' ? '#52c41a' : '#1890ff'">
                      <a-avatar :src="learner.avatar" v-if="learner.avatar" />
                      <a-avatar :src="learner.avatar" v-else>
                        <template #icon>
                          <SmileOutlined />
                        </template>
                      </a-avatar>
                    </a-badge>
                  </a-tooltip>
                  <div class="learner-name">{{ learner.nickname }}</div>
                  <div class="learner-status">
                    <a-tag :color="learner.status === 'completed' ? 'success' : 'processing'">
                      {{ learner.status === 'completed' ? '已完成' : '学习中' }}
                    </a-tag>
                  </div>
                </div>
              </div>
              <div v-if="courseDetail.learners && courseDetail.learners.length > 10" class="view-more">
                <a-button type="link">查看更多</a-button>
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 底部 -->
    <common-footer />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import CommonFooter from '@/components/common/FooterComponent.vue';
import { getCourseDetail } from '@/api/toc/course';
import { recordLearningProgress } from '@/api/toc/learning';
import { throttle } from 'throttle-debounce';
import { CheckCircleOutlined, SmileOutlined } from '@ant-design/icons-vue';
import { onUnmounted } from 'vue';

// 点击外部指令
const clickOutside = {
  beforeMount(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event);
      }
    };
    document.addEventListener('click', el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener('click', el.clickOutsideEvent);
  },
};

export default defineComponent({
  name: 'CourseDetailPage',
  components: {
    HeaderComponent,
    CommonFooter,
    CheckCircleOutlined,
    SmileOutlined
  },
  directives: {
    'click-outside': clickOutside
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const videoPlayer = ref(null);
    const currentPlaybackRate = ref(1);
    const currentCourseIndex = ref(0);
    // 记录进度interval对象
    const intervalRecordPageProgressObj = ref(null);

    // 状态
    const loading = ref(false);

    /** 课程详情 */
    const courseDetail = ref({});
    /** 当前课程 */
    const currentCourse = ref({});
    /** 当前课程最大播放时间 */
    const maxWatchTime = ref(0);

    // 方法
    const fetchCourseDetail = async () => {
      const courseId = route.params.id;
      if (!courseId) {
        message.error('课程ID不能为空');
        return;
      }

      loading.value = true;
      try {
        const response = await getCourseDetail(courseId);
        if (response.code === 200 && response.data) {
          courseDetail.value = response.data;
          if (response.data.type === 'series') {
            /** 系列课立即记录一次进度 */
            updatePageProgress(0, true);
            /** 进来就切换到第一课 */
            switchCourse(response.data?.courseItems[0]);
          } else {
            /** 普通课程从外层取数据 */
            currentCourse.value = response.data;
            updatePageProgress(100);
          }
        } else {
          message.error(response.message || '获取课程详情失败');
        }
      } catch (error) {
        console.error('获取课程详情异常:', error);
      } finally {
        loading.value = false;
      }
    };

    const formatDate = (dateStr) => {
      if (!dateStr) return '';

      try {
        const date = new Date(dateStr);
        return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
      } catch (error) {
        return dateStr;
      }
    };

    const getTypeText = (type) => {
      const typeMap = {
        'video': '视频课程',
        'document': '文档',
        'series': '系列课程',
        'article': '文章'
      };
      return typeMap[type] || type;
    };

    const setPlaybackRate = (rate) => {
      if (videoPlayer.value) {
        videoPlayer.value.playbackRate = rate;
        currentPlaybackRate.value = rate;
      }
    };

    const switchCourse = (item) => {
      if (currentCourse.id === item.id) { return }
      // 切换到新课程
      currentCourse.value = item;

      // 保存当前课程的学习进度
      if (videoPlayer.value && currentCourse.value.type === 'video') {
        const progress = (videoPlayer.value.currentTime / videoPlayer.value.duration) * 100;
        updateCourseProgress(progress);
      }

      /** 如果是文档，则直接更新进度 */
      if (item.type === 'document') {
        updatePageProgress(100);
      }

      // 重置视频播放器
      if (videoPlayer.value) {
        videoPlayer.value.currentTime = 0;
      }
    };

    /**
     * 更新课程学习进度
     */
    const updatePageProgress = async (progress, isSeriesInit = false, duration) => {
      try {
        const isSeries = courseDetail.value.type === 'series';
        const data = {
          "contentType": isSeries ? 'APPENDIX_FILE' : 'COURSE',  // 附件类型
          "parentType": isSeries ? 'SERIES' : '',  // COURSE-课程/SERIES-系列课程/LEARNING_MAP-学习地图/MAP_STAGE-地图阶段/TRAIN-培训
          "contentId": currentCourse.value.id, // 当前节点id, 例如培训场景，这里传课程id
          "parentId": isSeries ? courseDetail.value.id : '', // 父节点id，例如培训场景，这里传培训id
          "progress": Math.floor(progress), // 进度
          "duration": duration || 0 // 学习时长 秒数
        }
        /** 如果是系列课初始化，则需要记录系列课的进度 */
        if (isSeriesInit) {
          data.contentType = 'SERIES';
          data.contentId = courseDetail.value.id;
          data.parentType = '';
          data.parentId = '';
        }
        await recordLearningProgress(data);
      } catch (error) {
        console.error('记录学习进度失败:', error);
      }
    };

    /**
     * 更新视频课程学习进度
     */
    const updateCourseProgress = throttle(3 * 1000, async (progress) => {
      try {
        const isSeries = courseDetail.value.type === 'series';
        await recordLearningProgress(
          {
            "contentType": isSeries ? 'APPENDIX_FILE' : 'COURSE',  // 附件类型
            "parentType": isSeries ? 'SERIES' : '',  // COURSE-课程/SERIES-系列课程/LEARNING_MAP-学习地图/MAP_STAGE-地图阶段/TRAIN-培训
            "contentId": currentCourse.value.id, // 当前节点id, 例如培训场景，这里传课程id
            "parentId": isSeries ? courseDetail.value.id : '', // 父节点id，例如培训场景，这里传培训id
            "progress": Math.floor(progress), // 进度
            "duration": Math.floor(videoPlayer.value ? videoPlayer.value.currentTime : 0) // 学习时长 秒数
          }
        );

        // 更新当前课程项的进度
        if (currentCourse.value.type === 'series') {
          currentCourse.value.progress = Math.floor(progress);

          // 计算整体系列课程的进度
          const totalProgress = currentCourse.value.courseItems.reduce((sum, item) => sum + (item.progress || 0), 0) / courseDetail.courseItems.length;
          currentCourse.value.progress = Math.floor(totalProgress);
        } else {
          currentCourse.value.userProgress.progress = Math.floor(progress);
        }
      } catch (error) {
        console.error('记录学习进度失败:', error);
      }
    }, { leading: true, trailing: true });

    /**
     * 记录学习进度
     */
    const handleVideoProgress = () => {
      /** 记录当前播放最大时间，如果往后拖拽且大于最大进度，则不更新 */
      if (!videoPlayer.value) return;
      if (videoPlayer.value.currentTime > maxWatchTime.value && videoPlayer.value.currentTime > maxWatchTime.value + 1) {
        videoPlayer.value.currentTime = maxWatchTime.value;
        return
      }
      /** 记录当前播放最大时间 */
      if (videoPlayer.value.currentTime > maxWatchTime.value) {
        maxWatchTime.value = videoPlayer.value.currentTime;
      }

      const progress = (videoPlayer.value.currentTime / videoPlayer.value.duration) * 100;
      const currentProgress = courseDetail.value.type === 'series'
        ? currentCourse.value?.progress || 0
        : currentCourse.value?.userProgress.progress || 0;

      if (progress > currentProgress) {
        updateCourseProgress(progress);
      }
    };

    // 15秒记录一次学习进度
    const intervalRecordPageProgress = () => {
      intervalRecordPageProgressObj.value = setInterval(() => {
        updatePageProgress(100, courseDetail.value.type === 'series', 15);
      }, 15 * 1000);
    };

    // 监听当前课程变化，重置播放速度
    watch(currentCourseIndex, () => {
      currentPlaybackRate.value = 1;
      if (videoPlayer.value) {
        videoPlayer.value.playbackRate = 1;
      }
    });

    // 生命周期钩子
    onMounted(() => {
      fetchCourseDetail();
      intervalRecordPageProgress();
    });

    onUnmounted(() => {
      clearInterval(intervalRecordPageProgressObj.value);
    });

    return {
      loading,
      courseDetail,
      currentCourse,
      currentCourseIndex,
      videoPlayer,
      currentPlaybackRate,
      formatDate,
      getTypeText,
      setPlaybackRate,
      handleVideoProgress,
      switchCourse
    };
  }
});
</script>

<style scoped lang="less">
.course-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
}

.main-content {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.course-detail-container {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.course-header {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.course-title h1 {
  font-size: 24px;
  font-weight: bold;
  color: #111827;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #6b7280;
  font-size: 14px;
}

.creator-info {
  color: #4b5563;
}

.progress-info {
  color: #2563eb;
  font-weight: 500;
}

.course-content {
  display: flex;
  justify-content: center;

  &.with-sidebar {
    .content-display {
      flex: 1;
    }
  }
}

.course-sidebar {
  width: 280px;
  border-right: 1px solid #e5e7eb;
  background-color: #f9fafb;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid #e5e7eb;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #111827;
    }
  }

  .sidebar-content {
    overflow-y: auto;
    max-height: 600px;
  }

  .sidebar-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #e5e7eb;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f3f4f6;
    }

    &.active {
      background-color: #eff6ff;
      border-left: 3px solid #2563eb;
    }

    .item-index {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background-color: #e5e7eb;
      color: #4b5563;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      margin-right: 12px;
      flex-shrink: 0;
    }

    .item-info {
      flex: 1;
      overflow: hidden;

      .item-title {
        font-size: 14px;
        font-weight: 500;
        color: #111827;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .item-type {
        font-size: 12px;
        color: #6b7280;
      }
    }

    .item-progress {
      margin-left: 12px;

      .progress-circle {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background-color: #e5e7eb;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          top: 2px;
          left: 2px;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background-color: #fff;
        }
      }
    }
  }
}

.content-display {
  width: 100%;
  min-height: 600px;
}

.video-container {
  position: relative;
  width: 100%;
  background-color: #000;

  &:hover {
    .video-controls {
      display: block;
    }
  }
}

.video-player {
  width: 100%;
  height: 600px;
  background-color: #000;
}

.video-controls {
  display: none;
  position: absolute;
  bottom: 36px;
  right: 136px;
  z-index: 10;
  width: 60px;
  text-align: center;
}

.speed-control {
  position: relative;
  user-select: none;

  &:hover {
    .speed-options {
      display: block;
    }
  }
}

.current-speed {
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;

  &:hover {
    background-color: rgba(0, 0, 0, 0.7);
  }
}

.speed-options {
  position: absolute;
  bottom: 100%;
  right: 0;
  // margin-bottom: 8px;
  background-color: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
  overflow: hidden;
  width: 60px;
  display: none;
}

.speed-option {
  color: white;
  padding: 8px 12px;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(255, 255, 255, 0.2);
  }

  &.active {
    background-color: rgba(24, 144, 255, 0.6);
    font-weight: 500;
  }
}

.document-container {
  width: 100%;
  height: 800px;
}

.pdf-viewer {
  width: 100%;
  height: 100%;
  border: none;
}


.learners-section {
  margin-bottom: 40px;
  border-top: 1px solid #e5e7eb;
  padding: 24px;
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
  ;
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
