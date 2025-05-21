<template>
  <a-layout class="train-detail-page">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent
        v-model:collapsed="collapsed"
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal"
      />

      <!-- 内容区 -->
      <a-layout-content class="content">

          <!-- 标题栏 -->
          <div class="content-header">
            <PageBreadcrumb />
          </div>
          <!-- 顶部信息区 -->
          <a-page-header
            :title="trainDetail.name"
            :sub-title="`创建人: ${trainDetail.creatorName || '-'}`"
          >
            <template #extra>
              <a-space>
                <a-button v-if="isAdmin" type="primary" @click="goToEditPage">
                  <template #icon><edit-outlined /></template>
                  编辑
                </a-button>
              </a-space>
            </template>
          </a-page-header><a-row :gutter="24" class="main-content">
            <!-- 左侧内容区 --><a-col :span="18">
              <!-- 封面图区 -->
              <a-card :bordered="false" class="cover-card">
                <a-image
                  v-if="trainDetail.cover"
                  :src="trainDetail.cover"
                  :preview="false"
                  alt="培训封面"
                  class="cover-image"
                />
                <div v-else class="no-cover">
                  <picture-outlined />
                  <p>暂无封面图</p>
                </div></a-card>

              <!-- 培训简介 -->
              <a-card :bordered="false" title="培训简介" class="intro-card">
                <div v-if="trainDetail.introduction" class="introduction">
                  {{ trainDetail.introduction }}
                </div>
                <a-empty v-else description="暂无简介" /></a-card>

              <!-- 内容列表区 -->
              <a-card :bordered="false" title="培训内容" class="content-card">
                <a-collapse v-if="trainDetail.contents && trainDetail.contents.length > 0"><a-collapse-panel
                    v-for="(content, index) in trainDetail.contents"
                    :key="index"
                    :header="content.title">
                    <div class="content-item">
                      <div class="content-info">
                        <p>
                          <tag-outlined />类型: <a-tag :color="getContentTypeColor(content.type)">
                            {{ getContentTypeText(content.type) }}</a-tag>
                        </p>
                        <p>
                          <check-circle-outlined />
                          {{ content.isRequired ? '必修' : '选修' }}
                        </p>
                      </div>
                      <div class="content-actions">
                        <a-button
                          type="primary"
                          @click="viewContent(content)">
                          查看内容
                        </a-button>
                      </div>
                    </div>
                  </a-collapse-panel>
                </a-collapse>
                <a-empty v-else description="暂无培训内容" />
              </a-card></a-col>

            <!-- 右侧信息侧边栏 -->
            <a-col :span="6">
              <a-card :bordered="false" title="培训信息" class="info-card">
                <p>
                  <trophy-outlined /> 学分: <a-tag color="blue">{{ trainDetail.credit ||0 }}</a-tag>
                </p>
                <p>
                  <calendar-outlined /> 创建时间:{{ formatDate(trainDetail.gmtCreate) }}
                </p>
                <p>
                  <flag-outlined /> 状态: <a-tag :color="getStatusColor(trainDetail.status)">
                    {{ getStatusText(trainDetail.status) }}
                  </a-tag>
                </p>
                <p>
                  <message-outlined /> 评论:
                  {{ trainDetail.allowComment ? '允许' : '不允许' }}
                </p>
              </a-card>

              <a-card :bordered="false" title="分类信息" class="category-card">
                <div v-if="trainDetail.categoryNames && trainDetail.categoryNames.length > 0"><a-tag
                    v-for="(category, index) in trainDetail.categoryNames"
                    :key="index"
                    color="blue"
                    class="category-tag"
                  >
                    {{ category }}</a-tag>
                </div><a-empty v-else description="暂无分类" />
              </a-card>

              <a-card :bordered="false" title="证书信息" class="certificate-card">
                <div v-if="trainDetail.certificateName">
                  <p>
                    <safety-certificate-outlined /> 证书名称:
                    {{ trainDetail.certificateName }}
                  </p>
                </div>
                <a-empty v-else description="暂无证书" /></a-card>
            </a-col>
          </a-row>

      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { getTrainDetail } from '@/api/train';
import {
  UserOutlined, SettingOutlined,
  LogoutOutlined,
  DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  EditOutlined,
  PictureOutlined,
  TagOutlined,
  CheckCircleOutlined,
  CalendarOutlined,
  FlagOutlined,
  MessageOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import { hasPermission } from '@/utils/permission';
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue';

export default defineComponent({
  name: 'TrainDetailPage',
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
    PictureOutlined,
    TagOutlined,
    CheckCircleOutlined,
    CalendarOutlined,
    FlagOutlined,
    MessageOutlined,
    SafetyCertificateOutlined,
    PageBreadcrumb,
    CollaboratorSelectionModal
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');
    const router = useRouter();
    const route = useRoute();
    const trainId = ref(route.params.id);
    const collapsed = ref(false);
    const selectedKeys = ref(['trainList']);
    const openKeys = ref(['learning']);

    // 培训详情
    const trainDetail = ref({
      id: null,
      title: '',
      cover: '',
      introduction: '',
      credit: 0,
      categoryIds: [],
      categoryNames: [],
      allowComment: false,
      certificateId: null,
      certificateName: '',
      status: '',
      creatorId: null,
      creatorName: '',
      gmtCreate: null,
      contents: [],
      visibility: {
        type: '',
        targets: {}
      },
      collaborators: {
        editors: [],
        users: []
      }
    });

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });

    // 加载状态
    const loading = ref(false);

    // 获取培训详情
    const fetchTrainDetail = async () => {
      loading.value = true;
      try {
        const res = await getTrainDetail(trainId.value);
        if (res.code === 200 && res.data) {
          trainDetail.value = res.data;
        } else {
          message.error(res.message || '获取培训详情失败');
        }
      } catch (error) {
        console.error('获取培训详情失败:', error);
      } finally {
        loading.value = false;
      }
    };// 获取用户信息
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

    // 判断是否为管理员
    const isAdmin = computed(() => {
      // 判断当前用户是否为培训创建者或有编辑权限
      return userInfo.value.userId === trainDetail.value.creatorId ||
             (trainDetail.value.collaborators &&
              trainDetail.value.collaborators.editors &&
              trainDetail.value.collaborators.editors.includes(userInfo.value.userId));
    });

    // 格式化日期
    const formatDate = (date) => {
      if (!date) return '-';
      return dayjs(date).format('YYYY-MM-DD HH:mm');
    };

    // 获取内容类型文本
    const getContentTypeText = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '调研',
        'CONTENT': '内容'
      };
      return typeMap[type] || type;
    };

    // 获取内容类型颜色
    const getContentTypeColor = (type) => {
      const colorMap = {
        'COURSE': 'blue',
        'EXAM': 'red',
        'ASSIGNMENT': 'orange',
        'SURVEY': 'green',
        'CONTENT': 'purple'
      };
      return colorMap[type] || 'default';
    };// 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'draft': '草稿',
        'published': '已发布',
        'unpublished': '未发布',
        'archived': '已归档'
      };
      return statusMap[status] || status;
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        'draft': 'default',
        'published': 'success',
        'unpublished': 'warning',
        'archived': 'processing'
      };
      return statusMap[status] || 'default';
    };// 查看内容
    const viewContent = (content) => {
      // 根据内容类型跳转到不同的页面
      switch (content.type) {
        case 'COURSE':
          router.push(`/course/detail/${content.contentId}`);
          break;
        case 'EXAM':
          // 跳转到考试页面
          window.open(content.contentUrl, '_blank');
          break;
        case 'ASSIGNMENT':
          // 跳转到作业页面
          window.open(content.contentUrl, '_blank');
          break;
        case 'SURVEY':
          // 跳转到调研页面
          window.open(content.contentUrl, '_blank');
          break;
        case 'CONTENT':
          // 跳转到内容页面
          window.open(content.contentUrl, '_blank');
          break;
        default:
          message.warning('暂不支持查看该类型内容');
      }
    };// 返回上一页
    const goBack = () => {
      router.push('/train/list');
    };

    //跳转到编辑页面
    const goToEditPage = () => {
      router.push(`/train/edit/${trainId.value}`);
    };

    // 页面跳转
    const navigateTo = (path) => {
      router.push(path);
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

    onMounted(() => {
      // 获取用户信息
      getUserInfo();// 获取培训详情
      fetchTrainDetail();
    });
    return {
      trainId,
      trainDetail,
      userInfo,
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      isAdmin,
      formatDate,
      getContentTypeText,
      getContentTypeColor,
      getStatusText,
      getStatusColor,
      viewContent,
      goBack,
      goToEditPage,
      navigateTo,
      handleLogout
    };
  }
});
</script>

<style scoped>
.train-detail-page {
  min-height: 100vh;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: hidden;
}

.content-header{
  padding: 0;
}

.main-content {
  height: calc(100% - 60px);
  overflow-y: auto;
}

.cover-card {
  margin-bottom: 24px;
}

.cover-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
}

.no-cover {
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  color: #999;
}

.no-cover .anticon {
  font-size: 48px;
  margin-bottom: 16px;
}

.intro-card {
  margin-bottom: 24px;
}

.introduction {
  white-space: pre-wrap;line-height: 1.6;
}

.content-card {
  margin-bottom: 24px;
}

.content-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-info {
  flex: 1;
}

.content-info p {
  margin-bottom: 8px;
}

.content-actions {
  margin-left: 16px;
}

.info-card,
.category-card,
.certificate-card {
  margin-bottom: 24px;
}

.info-card p {
  margin-bottom: 12px;
}

.category-tag {
  margin-bottom: 8px;
  margin-right: 8px;
}
</style>
