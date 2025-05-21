<template>
  <div class="course-detail-mobile-page">
    <!-- 主体内容 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 课程详情 -->
        <div class="course-detail-container">
          <!-- 课程头部信息 -->
          <div class="course-header">
            <div class="course-meta">
              <div class="creator-info">
                <span v-if="currentCourse?.instructor?.name">
                  <user-outlined />
                  {{ currentCourse?.instructor?.name }}
                </span>
              </div>
              <div class="date-info">
                <calendar-outlined />
                {{ formatDate(currentCourse.createdAt) }}
              </div>
              <div class="progress-info" v-if="courseDetail.type === 'series'">
                <progress-outlined />
                已学 {{ currentCourse?.progress || 0 }}%
              </div>
              <div class="progress-info" v-else>
                <progress-outlined />
                已学 {{ currentCourse.userProgress?.progress || 0 }}%
              </div>
            </div>
          </div>

          <!-- 内容展示区 -->
          <div class="content-display">
            <!-- 视频播放器 -->
            <div v-if="currentCourse.type === 'video'" class="video-container">
              <video ref="videoPlayer" class="video-player" controls :src="currentCourse.appendixPath"
                @timeupdate="handleVideoProgress" playsinline webkit-playsinline>
                <source :src="currentCourse.appendixPath" type="video/mp4">
                您的浏览器不支持视频播放
              </video>
              <div class="video-controls">
                <div class="speed-control">
                  <div class="current-speed">
                    倍速播放
                  </div>
                  <div class="speed-options">
                    <div v-for="rate in [1, 1.25, 1.5, 2]" :key="rate"
                      :class="['speed-option', { active: currentPlaybackRate === rate }]"
                      @click.stop="setPlaybackRate(rate)">
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

          <!-- 系列课程目录 -->
          <div v-if="courseDetail.type === 'series'" class="course-catalog">
            <div class="catalog-header">
              <h3>课程目录</h3>
            </div>
            <div class="catalog-content">
              <div v-for="(item, index) in courseDetail.courseItems" :key="item.id"
                :class="['catalog-item', { active: currentCourse.id === item.id }]" @click="switchCourse(item)">
                <div class="item-index">{{ index + 1 }}</div>
                <div class="item-info">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-type">{{ getTypeText(item.type) }}</div>
                </div>
                <div class="item-status">
                  <check-circle-filled v-if="item.status === 'completed'" class="completed-icon" />
                  <div v-else class="progress-circle">{{ item.progress || 0 }}%</div>
                </div>
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

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />

    <!-- 悬浮目录按钮 -->
    <div v-if="courseDetail.type === 'series'" class="floating-catalog-button" @click="toggleCatalogModal">
      <unordered-list-outlined />
    </div>

    <!-- 简略目录弹窗 -->
    <a-modal v-model:visible="showCatalogModal" :footer="null" :closable="true" :mask-closable="true"
      class="catalog-modal" title="课程目录">
      <div class="mini-catalog-list">
        <div v-for="(item, index) in courseDetail.courseItems" :key="item.id"
          :class="['mini-catalog-item', { active: currentCourse.id === item.id }]" @click="switchCourseAndClose(item)">
          <div class="mini-item-index">{{ index + 1 }}</div>
          <div class="mini-item-title">{{ item.title }}</div>
          <div class="mini-item-status">
            <check-circle-filled v-if="item.status === 'completed'" class="mini-completed-icon" />
            <div v-else class="mini-progress">{{ item.progress || 0 }}%</div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, watch, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import { getCourseDetail } from '@/api/toc/course';
import { recordLearningProgress } from '@/api/toc/learning';
import { throttle } from 'throttle-debounce';
import {
  UserOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ProgressOutlined,
  UnorderedListOutlined,
  SmileOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CourseDetailMobilePage',
  components: {
    MobileTabBar,
    UserOutlined,
    CalendarOutlined,
    CheckCircleFilled,
    ProgressOutlined,
    UnorderedListOutlined,
    SmileOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const videoPlayer = ref(null);
    const currentPlaybackRate = ref(1);
    const showSpeedOptions = ref(false);
    const showCatalogModal = ref(false);
    /** 记录进度interval对象 */
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
            /** 系列课程从courseItems取数据 */
            currentCourse.value = response.data?.courseItems[0];
            /** 系列课立即记录一次进度 */
            updatePageProgress(0, true);
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

    // 切换课程
    const switchCourse = (course) => {
      if (currentCourse.value.id === course.id) return;

      // 保存当前课程的学习进度
      if (videoPlayer.value && currentCourse.value.type === 'video') {
        const progress = (videoPlayer.value.currentTime / videoPlayer.value.duration) * 100;
        updateCourseProgress(progress);
      }

      /** 如果是文档，则直接更新进度 */
      if (course.type === 'document') {
        updatePageProgress(100);
      }

      // 切换课程
      currentCourse.value = course;

      // 重置视频播放器状态
      if (videoPlayer.value) {
        videoPlayer.value.currentTime = 0;
        maxWatchTime.value = 0;
      }
    };

    // 处理视频进度
    const handleVideoProgress = throttle(1000, () => {
      if (!videoPlayer.value) return;

      const currentTime = videoPlayer.value.currentTime;
      const duration = videoPlayer.value.duration;

      if (isNaN(duration) || duration <= 0) return;

      // 更新最大观看时间
      if (currentTime > maxWatchTime.value) {
        maxWatchTime.value = currentTime;

        // 计算进度百分比
        const progressPercent = Math.floor((currentTime / duration) * 100);

        // 记录学习进度
        updateCourseProgress(progressPercent);
      }
    });


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

    // 设置播放速率
    const setPlaybackRate = (rate) => {
      if (!videoPlayer.value) return;

      currentPlaybackRate.value = rate;
      videoPlayer.value.playbackRate = rate;
      showSpeedOptions.value = false;
    };

    // 切换速度选项显示
    const toggleSpeedOptions = () => {
      showSpeedOptions.value = !showSpeedOptions.value;
    };

    // 点击外部关闭速度选项
    const handleClickOutside = (event) => {
      const speedControl = document.querySelector('.speed-control');
      if (speedControl && !speedControl.contains(event.target)) {
        showSpeedOptions.value = false;
      }
    };

    // 切换目录弹窗显示状态
    const toggleCatalogModal = () => {
      showCatalogModal.value = !showCatalogModal.value;
    };

    // 切换课程并关闭弹窗
    const switchCourseAndClose = (course) => {
      switchCourse(course);
      showCatalogModal.value = false;
    };

    // 15秒记录一次学习进度
    const intervalRecordPageProgress = () => {
      intervalRecordPageProgressObj.value = setInterval(() => {
        updatePageProgress(100, courseDetail.value.type === 'series', 15);
      }, 15 * 1000);
    };

    onMounted(() => {
      fetchCourseDetail();
      intervalRecordPageProgress();
      document.addEventListener('click', handleClickOutside);
    });

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside);
      // 离开页面时记录进度
      updatePageProgress(100);
      clearInterval(intervalRecordPageProgressObj.value);
    });

    // 监听路由变化，重新获取课程详情
    watch(() => route.params.id, (newId, oldId) => {
      if (newId !== oldId) {
        fetchCourseDetail();
      }
    });

    return {
      loading,
      courseDetail,
      currentCourse,
      videoPlayer,
      currentPlaybackRate,
      showSpeedOptions,
      showCatalogModal,
      formatDate,
      getTypeText,
      switchCourse,
      switchCourseAndClose,
      handleVideoProgress,
      setPlaybackRate,
      toggleSpeedOptions,
      toggleCatalogModal
    };
  }
});
</script>

<style scoped>
.course-detail-mobile-page {
  padding-bottom: 65px;
}

.main-content {
  padding: 0;
}

.course-detail-container {
  background-color: #fff;
}

.course-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #666;
}

.creator-info,
.date-info,
.progress-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.content-display {
  width: 100%;
}

.video-container {
  position: relative;
  width: 100%;
  background-color: #000;
}

.video-player {
  width: 100%;
  height: auto;
  max-height: 230px;
  display: block;
}

.video-controls {
  bottom: 60px;
  right: 10px;
  z-index: 10;
}

.speed-control {
  position: relative;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 0px 10px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: start;
}

.current-speed {
  font-size: 12px;
  margin-right: 5px;
}

.speed-options {
  display: flex;
  bottom: 100%;
  right: 0;
  background-color: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 5px;
}

.speed-option {
  padding: 6px 12px;
  text-align: center;
  cursor: pointer;
}

.speed-option.active {
  color: #1890ff;
}

.document-container {
  width: 100%;
  height: 100vh;
  max-height: 500px;
  overflow: hidden;
}

.pdf-viewer {
  width: 100%;
  height: 100%;
  border: none;
}

.course-catalog {
  padding: 16px;
}

.catalog-header {
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.catalog-header h3 {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}

.catalog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.catalog-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background-color: #f9f9f9;
  cursor: pointer;
  transition: all 0.3s;
}

.catalog-item.active {
  background-color: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.item-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
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
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-type {
  font-size: 12px;
  color: #666;
}

.item-status {
  margin-left: 8px;
}

.completed-icon {
  color: #52c41a;
  font-size: 20px;
}

.progress-circle {
  font-size: 12px;
  color: #1890ff;
}

/* 悬浮目录按钮样式 */
.floating-catalog-button {
  position: fixed;
  right: 16px;
  bottom: 80px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.floating-catalog-button:active {
  transform: scale(0.95);
  background-color: #096dd9;
}

/* 简略目录弹窗样式 */
:deep(.catalog-modal .ant-modal-content) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.catalog-modal .ant-modal-body) {
  padding: 12px;
  max-height: 60vh;
  overflow-y: auto;
}

.mini-catalog-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-catalog-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  background-color: #f5f5f5;
  cursor: pointer;
  transition: background-color 0.3s;
}

.mini-catalog-item:active {
  background-color: #e6f7ff;
}

.mini-catalog-item.active {
  background-color: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.mini-item-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 8px;
  flex-shrink: 0;
}

.mini-item-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-item-status {
  margin-left: 8px;
  flex-shrink: 0;
}

.mini-completed-icon {
  color: #52c41a;
}

.mini-progress {
  font-size: 12px;
  color: #1890ff;
}

.learners-section {
  margin-bottom: 40px;
  border-top: 1px solid #e5e7eb;
  padding: 12px;
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
  grid-template-columns: repeat(auto-fill, minmax(60px, 1fr));
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