<template>
  <a-layout class="course-detail-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" /><a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <!-- 标题栏 -->
        <div class="content-header">
          <PageBreadcrumb />
        </div>
        <!-- 页面头部1 -->
        <a-page-header :title="courseDetail.title" :sub-title="getTypeText(courseDetail.type)" class="page-header">
          <template #extra>
            <a-space>
              <a-button v-if="hasPermission('course:edit')" type="primary" @click="handleEdit">
                <edit-outlined />编辑
              </a-button>
              <!-- <a-button type="primary" @click="navigateTo(`/course/analysis/${courseId}`)">
                <bar-chart-outlined />数据分析
              </a-button> -->
              <a-button @click="navigateTo('/course/list')">
                <rollback-outlined />返回列表</a-button>
            </a-space>
          </template>
          <template #tags>
            <a-tag :color="courseDetail.status === 'published' ? 'green' : 'orange'">
              {{ courseDetail.status === 'published' ? '已发布' : '未发布' }}
            </a-tag>
          </template>
          <a-descriptions size="small" :column="3">
            <a-descriptions-item label="创建人">{{ courseDetail.creatorName }}</a-descriptions-item><a-descriptions-item
              label="发布时间">{{ courseDetail.publishTime }}</a-descriptions-item>
            <a-descriptions-item label="查看数">{{ courseDetail.viewCount }}</a-descriptions-item>
            <a-descriptions-item label="学分">{{ courseDetail.credit }}</a-descriptions-item>
            <a-descriptions-item label="完成人数">{{ courseDetail.completeCount }}</a-descriptions-item>
            <a-descriptions-item label="分类">
              <template v-if="courseDetail.categoryNames && courseDetail.categoryNames.length"><a-tag
                  v-for="(category, index) in courseDetail.categoryNames" :key="index" color="blue">
                  {{ category }}
                </a-tag>
              </template>
              <span v-else>-</span>
            </a-descriptions-item></a-descriptions>
        </a-page-header>
        <a-row :gutter="16" class="course-content-container">
          <!-- 左侧内容区 -->
          <a-col :span="18">
            <a-card class="course-content-card">
              <!-- 封面图 -->
              <div class="course-cover">
                <a-image :src="courseDetail.coverImage || 'https://via.placeholder.com/800x400'" :preview="false"
                  alt="课程封面" />
              </div>

              <!-- 课程简介 -->
              <div class="course-description">
                <h3>课程简介</h3>
                <p>{{ courseDetail.description }}</p>
              </div>

              <!-- 课程内容 -->
              <div class="course-content">
                <h3>课程内容</h3>
                <!-- 文章类型 -->
                <div v-if="courseDetail.type === 'article'" class="article-content">
                  <div v-html="courseDetail.article"></div>
                </div>

                <!-- 视频类型 -->
                <div v-else-if="courseDetail.type === 'video'" class="video-content">
                  <video-player v-if="courseDetail.appendixPath" :src="courseDetail.appendixPath"
                    :poster="courseDetail.coverImage" :playback-rates="playbackRates" @play="handleVideoPlay"
                    @pause="handleVideoPause" @ended="handleVideoEnded" @timeupdate="handleVideoTimeUpdate"
                    ref="videoPlayerRef" />
                  <div v-else class="no-content">视频资源不可用</div>
                </div>

                <!-- 文档类型 -->
                <div v-else-if="courseDetail.type === 'document'" class="document-content">
                  <template v-if="courseDetail.appendixPath">
                    <div class="document-preview">
                      <iframe v-if="isPreviewable" :src="getDocumentViewerUrl(courseDetail.appendixPath)" width="100%"
                        height="600" frameborder="0"></iframe>
                      <div v-else class="document-download">
                        <file-outlined />
                        <p>此文档不支持在线预览，请下载后查看</p>
                        <a-button type="primary" @click="handleDownload">
                          <download-outlined style="font-size: 12px; color: #fff;" />下载文档
                        </a-button>
                      </div>
                    </div>
                  </template>
                  <div v-else class="no-content">文档资源不可用</div>
                </div>
                <!-- 系列课类型 -->
                <div v-else-if="courseDetail.type === 'series'" class="series-content"><template
                    v-if="seriesFiles.length > 0"><a-collapse v-model:activeKey="activeSeriesKey"><a-collapse-panel
                        v-for="(section, sectionIndex) in seriesSections" :key="sectionIndex"
                        :header="`第${sectionIndex + 1}章：${section.title}`">
                        <a-list size="small" :data-source="section.items">
                          <template #renderItem="{ item, index }"><a-list-item>
                              <a-list-item-meta><template #title><a @click="playSeriesItem(item)">
                                    {{ index + 1 }}. {{ item.name }}
                                  </a></template>
                                <template #avatar>
                                  <file-outlined v-if="item.type.includes('document')" /><video-camera-outlined
                                    v-else-if="item.type.includes('video')" /><file-unknown-outlined v-else />
                                </template><template #description>
                                  <span>{{ getFileTypeText(item.type) }}</span>
                                  <a-tag v-if="item.completed" color="green" style="margin-left: 8px">
                                    已完成
                                  </a-tag>
                                </template></a-list-item-meta><template #extra>
                                <a-button type="link" @click="playSeriesItem(item)"><play-circle-outlined
                                    v-if="item.type.includes('video')" />
                                  <eye-outlined v-else />查看
                                </a-button>
                              </template></a-list-item>
                          </template></a-list></a-collapse-panel>
                    </a-collapse>

                    <!-- 系列课内容播放区域 -->
                    <div v-if="currentSeriesItem" class="series-player">
                      <h4>{{ currentSeriesItem.name }}</h4><video-player v-if="currentSeriesItem.type.includes('video')"
                        :src="currentSeriesItem.path" :poster="courseDetail.coverImage" :playback-rates="playbackRates"
                        @play="handleVideoPlay" @pause="handleVideoPause" @ended="handleSeriesItemEnded"
                        @timeupdate="handleVideoTimeUpdate" ref="seriesVideoPlayerRef" />
                      <iframe v-else-if="isSeriesItemPreviewable" :src="getDocumentViewerUrl(currentSeriesItem.path)"
                        width="100%" height="600" frameborder="0"></iframe>
                      <div v-else class="document-download">
                        <file-outlined />
                        <p>此文档不支持在线预览，请下载后查看</p>
                        <a-button type="primary" @click="handleSeriesItemDownload">
                          <download-outlined />下载文档
                        </a-button>
                      </div>
                    </div>
                  </template>
                  <div v-else class="no-content">系列课内容不可用</div>
                </div>
                <div v-else class="no-content">暂无内容</div>
              </div>

              <!-- 评论区 -->
              <!-- <div v-if="courseDetail.allowComments" class="course-comments">
                <h3>评论区</h3><a-comment><template #avatar>
                    <a-avatar :src="userInfo.avatar || 'https://joeschmoe.io/api/v1/random'" :alt="userInfo.nickname" />
                  </template>
                  <template #content>
                    <a-form-item><a-textarea v-model:value="commentValue" :rows="4" /></a-form-item>
                    <a-form-item><a-button html-type="submit" type="primary" @click="handleComment">
                        添加评论</a-button></a-form-item>
                  </template>
                </a-comment>
                <a-list class="comment-list" :header="`${comments.length} 条评论`" item-layout="horizontal"
                  :data-source="comments"><template #renderItem="{ item }">
                    <a-list-item><a-comment :author="item.author" :avatar="item.avatar" :content="item.content"
                        :datetime="item.datetime" />
                    </a-list-item>
                  </template>
                </a-list>
              </div> -->
            </a-card></a-col>
          <!-- 右侧信息栏 -->
          <a-col :span="6">
            <a-card title="课程信息" class="course-info-card">
              <p><strong>学分：</strong> {{ courseDetail.credit }}</p>
              <p><strong>类型：</strong> {{ getTypeText(courseDetail.type) }}</p>
              <p><strong>创建人：</strong> {{ courseDetail.creatorName }}</p>
              <p><strong>发布时间：</strong> {{ courseDetail.publishTime }}</p>
              <p><strong>查看数：</strong> {{ courseDetail.viewCount }}</p>
              <p><strong>完成人数：</strong> {{ courseDetail.completeCount }}</p>
              <p>
                <strong>分类：</strong><template
                  v-if="courseDetail.categoryNames && courseDetail.categoryNames.length"><a-tag
                    v-for="(category, index) in courseDetail.categoryNames" :key="index" color="blue">
                    {{ category }}</a-tag>
                </template>
                <span v-else>-</span>
              </p>
              <a-divider />
              <!-- <a-button v-if="!isCompleted" type="primary" block @click="handleMarkComplete">
                标记为已完成
              </a-button>
              <a-button v-else type="default" block disabled>
                已完成学习
              </a-button> -->
            </a-card>
          </a-col>
        </a-row>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { getCourseDetail, markCourseComplete } from '@/api/course';
import { hasPermission } from '@/utils/permission';
import VideoPlayer from '@/components/common/VideoPlayer.vue';
import {
  UserOutlined, SettingOutlined,
  LogoutOutlined, DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  EditOutlined,
  RollbackOutlined,
  FileOutlined,
  DownloadOutlined,
  VideoCameraOutlined,
  FileUnknownOutlined,
  PlayCircleOutlined,
  EyeOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';

export default defineComponent({
  name: 'CourseDetailPage',
  components: {
    HeaderComponent,
    SiderComponent,
    UserOutlined,
    SettingOutlined,
    LogoutOutlined,
    DownOutlined,
    DashboardOutlined,
    ReadOutlined,
    SolutionOutlined,
    ClusterOutlined,
    TrophyOutlined,
    AppstoreOutlined,
    EditOutlined,
    RollbackOutlined,
    FileOutlined,
    DownloadOutlined,
    VideoCameraOutlined,
    FileUnknownOutlined,
    PlayCircleOutlined,
    EyeOutlined,
    BarChartOutlined,
    VideoPlayer,
    PageBreadcrumb
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const route = useRoute();
    const courseId = ref(parseInt(route.params.id) || 0);
    const loading = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['courseList']);
    const openKeys = ref(['learning']);

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });// 权限列表


    // 课程详情
    const courseDetail = ref({
      id: 0,
      title: '',
      type: '',
      coverImage: '',
      instructorId: null,
      description: '',
      credit: 0,
      categoryIds: '',
      categoryNames: [],
      status: '',
      allowComments: true,
      isTop: false,
      viewCount: 0,
      completeCount: 0,
      article: '',
      appendixType: '',
      appendixPath: '',
      publishTime: '',
      creatorId: null,
      creatorName: ''
    });

    // 视频播放速度选项
    const playbackRates = ref([0.5, 1.0, 1.5, 2.0]);// 视频播放器引用
    const videoPlayerRef = ref(null);
    const seriesVideoPlayerRef = ref(null);

    // 系列课文件列表
    const seriesFiles = ref([]);// 系列课章节
    const seriesSections = ref([]);

    // 当前播放的系列课项目
    const currentSeriesItem = ref(null);// 系列课折叠面板激活的key
    const activeSeriesKey = ref([0]);

    // 是否可以预览文档
    const isPreviewable = computed(() => {
      if (!courseDetail.value.appendixPath) return false;
      const path = courseDetail.value.appendixPath.toLowerCase();
      return path.endsWith('.pdf') || path.endsWith('.doc') || path.endsWith('.docx') ||
        path.endsWith('.ppt') || path.endsWith('.pptx');
    });

    // 当前系列课项目是否可预览
    const isSeriesItemPreviewable = computed(() => {
      if (!currentSeriesItem.value || !currentSeriesItem.value.path) return false;
      const path = currentSeriesItem.value.path.toLowerCase();
      return path.endsWith('.pdf') || path.endsWith('.doc') || path.endsWith('.docx') ||
        path.endsWith('.ppt') || path.endsWith('.pptx');
    });

    // 是否已完成学习
    const isCompleted = ref(false);

    // 评论内容
    const commentValue = ref('');

    // 评论列表
    const comments = ref([]);

    // 获取用户信息
    const getUserInfo = () => {
      const userInfoStr = sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          userInfo.value = JSON.parse(userInfoStr);
        } catch (error) {
          console.error('解析用户信息失败:', error);
        }
      }
    };





    // 获取课程详情
    const fetchCourseDetail = async () => {
      if (!courseId.value) {
        message.error('课程ID不存在');
        router.push('/course/list');
        return;
      }

      loading.value = true;
      try {
        const res = await getCourseDetail(courseId.value);
        if (res.code === 200 && res.data) {
          courseDetail.value = res.data;

          // 处理系列课文件
          if (res.data.type === 'series' && res.data.seriesFiles) {
            seriesFiles.value = res.data.seriesFiles.map((file, index) => ({
              id: index,
              name: getFileNameFromPath(file.path),
              path: file.path,
              type: file.type,
              completed: false
            }));
            // 生成系列课章节
            generateSeriesSections();
          }

          // 记录查看数据
          recordViewData();
        } else {
          message.error(res.message || '获取课程详情失败');
          router.push('/course/list');
        }
      } catch (error) {
        console.error('获取课程详情失败:', error);
        router.push('/course/list');
      } finally {
        loading.value = false;
      }
    };

    // 生成系列课章节
    const generateSeriesSections = () => {
      // 简单示例：将所有文件分为一个章节
      seriesSections.value = [
        {
          title: '课程内容',
          items: seriesFiles.value
        }
      ];
    };// 从路径中获取文件名
    const getFileNameFromPath = (path) => {
      if (!path) return '';
      const parts = path.split('/');
      return parts[parts.length - 1];
    };

    // 获取课程类型文本
    const getTypeText = (type) => {
      const typeMap = {
        video: '视频',
        document: '文档',
        series: '系列课',
        article: '文章'
      };
      return typeMap[type] || type;
    };// 获取文件类型文本
    const getFileTypeText = (type) => {
      if (type.includes('video')) return '视频';
      if (type.includes('pdf')) return 'PDF文档';
      if (type.includes('doc')) return 'Word文档';
      if (type.includes('ppt')) return 'PPT演示文稿';
      if (type.includes('xls')) return 'Excel表格';
      return '文档';
    };

    // 获取文档查看器URL
    const getDocumentViewerUrl = (path) => {
      // 这里可以根据实际情况使用不同的文档查看器
      // 例如Google Docs Viewer或Microsoft Office Online
      return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(path)}`;
    };

    // 记录查看数据
    const recordViewData = async () => {
      try {
        // 调用后端API记录查看数据
        // 这里我们假设后端在调用getCourseDetail时会自动记录查看数据
        // 如果需要单独记录，可以添加一个专门的API调用
        console.log('记录课程查看数据，课程ID:', courseId.value);
      } catch (error) {
        console.error('记录查看数据失败:', error);
      }
    };

    // 处理编辑
    const handleEdit = () => {
      router.push(`/course/edit/${courseId.value}`);
    };

    // 处理下载
    const handleDownload = () => {
      if (courseDetail.value.appendixPath) {
        window.open(courseDetail.value.appendixPath, '_blank');
      }
    };

    // 处理系列课项目下载
    const handleSeriesItemDownload = () => {
      if (currentSeriesItem.value && currentSeriesItem.value.path) {
        window.open(currentSeriesItem.value.path, '_blank');
      }
    };

    // 播放系列课项目
    const playSeriesItem = (item) => {
      currentSeriesItem.value = item;
    };

    // 处理视频播放
    const handleVideoPlay = () => {
      console.log('视频开始播放');
    };

    // 处理视频暂停
    const handleVideoPause = () => {
      console.log('视频暂停');
    };

    // 处理视频结束
    const handleVideoEnded = () => {
      console.log('视频播放结束');// 可以在这里自动标记为已完成
      if (!isCompleted.value) {
        handleMarkComplete();
      }
    };// 处理系列课项目结束
    const handleSeriesItemEnded = () => {
      console.log('系列课项目播放结束');
      // 标记当前项为已完成
      if (currentSeriesItem.value) {
        const index = seriesFiles.value.findIndex(item => item.id === currentSeriesItem.value.id);
        if (index !== -1) {
          seriesFiles.value[index].completed = true;

          // 更新系列课章节中的项
          seriesSections.value.forEach(section => {
            const itemIndex = section.items.findIndex(item => item.id === currentSeriesItem.value.id);
            if (itemIndex !== -1) {
              section.items[itemIndex].completed = true;
            }
          });
        }
      }
      // 检查是否所有项都已完成
      const allCompleted = seriesFiles.value.every(item => item.completed);
      if (allCompleted && !isCompleted.value) {
        handleMarkComplete();
      }
    };// 处理视频时间更新
    const handleVideoTimeUpdate = (event) => {
      // 可以在这里记录学习进度
      console.log('视频播放进度:', event.target.currentTime);
    };

    // 标记为已完成
    const handleMarkComplete = async () => {
      try {
        // 调用后端API标记为已完成
        const res = await markCourseComplete(courseId.value);
        if (res.code === 200) {
          isCompleted.value = true;
          message.success('已标记为完成学习');

          // 更新完成人数
          if (courseDetail.value.completeCount !== undefined) {
            courseDetail.value.completeCount += 1;
          }
        } else {
          message.error(res.message || '标记完成失败');
        }
      } catch (error) {
        console.error('标记完成失败:', error);
      }
    };

    // 处理评论
    const handleComment = () => {
      if (!commentValue.value.trim()) {
        message.warning('请输入评论内容');
        return;
      }

      // 添加评论
      comments.value.unshift({
        author: userInfo.value.nickname || '用户',
        avatar: userInfo.value.avatar || 'https://joeschmoe.io/api/v1/random',
        content: commentValue.value,
        datetime: dayjs().format('YYYY-MM-DD HH:mm:ss')
      });

      // 清空评论框
      commentValue.value = '';
      message.success('评论成功');
    };

    // 退出登录
    const handleLogout = () => {
      // 清除登录信息
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('userInfo');
      localStorage.removeItem('rememberMe');
      message.success('已退出登录');

      // 跳转到登录页
      router.push('/login');
    };

    // 页面跳转
    const navigateTo = (path) => {
      router.push(path);
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();
      // 获取课程详情
      fetchCourseDetail();
    });

    return {
      courseId,
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,

      hasPermission,
      courseDetail,
      playbackRates,
      videoPlayerRef,
      seriesVideoPlayerRef,
      seriesFiles,
      seriesSections,
      currentSeriesItem,
      activeSeriesKey,
      isPreviewable,
      isSeriesItemPreviewable,
      isCompleted,
      commentValue,
      comments,
      getTypeText,
      getFileTypeText,
      getDocumentViewerUrl,
      handleEdit,
      handleDownload,
      handleSeriesItemDownload,
      playSeriesItem,
      handleVideoPlay,
      handleVideoPause,
      handleVideoEnded,
      handleSeriesItemEnded,
      handleVideoTimeUpdate,
      handleMarkComplete,
      handleComment,
      handleLogout,
      navigateTo
    };
  }
});
</script>

<style scoped>
.course-detail-layout {
  min-height: 100vh;
}



.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: hidden;
}

.course-content-container {
  height: calc(100% - 160px);
  overflow-y: auto;
}

.course-content-card {
  margin-bottom: 24px;
}

.course-cover {
  margin-bottom: 24px;
  text-align: center;
}

.course-cover img {
  max-width: 100%;
  border-radius: 4px;
}

.course-description {
  margin-bottom: 24px;
}

.course-content {
  margin-bottom: 24px;
}

.article-content {
  background-color: #fff;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.video-content {
  background-color: #000;
  border-radius: 4px;
  overflow: hidden;
}

.document-content {
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.document-preview {
  min-height: 600px;
}

.document-download {
  padding: 48px;
  text-align: center;
}

.document-download .anticon {
  font-size: 48px;
  color: #1890ff;
  margin-bottom: 16px;
}

.series-content {
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  padding: 16px;
}

.series-player {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.no-content {
  padding: 48px;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  background-color: #fafafa;
  border-radius: 4px;
}

.course-comments {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.course-info-card {
  position: sticky;
  top: 24px;
}
</style>
