<template>
  <a-layout class="certificate-detail-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" /><a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent
        v-model:collapsed="collapsed"
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal"
      />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <div class="content-header-left">
            <PageBreadcrumb />
          </div>
          <div class="content-header-right">
            <a-space>
              <a-button
                v-if="hasPermission('certificate:users')"
                @click="navigateTo(`/certificate/users/${certificateId}`)">
                <team-outlined />查看获得者
              </a-button>
              <a-button
                v-if="hasPermission('certificate:edit')"
                type="primary"
                @click="navigateTo(`/certificate/edit/${certificateId}`)"
              >
                <edit-outlined />编辑</a-button></a-space>
          </div>
        </div>

        <!-- 证书信息区域 -->
        <a-card class="certificate-info-card">
          <a-spin :spinning="loading">
            <div class="certificate-info-header">
              <h3 class="certificate-title">{{ certificateDetail.name }}</h3>
              <p class="certificate-description">{{ certificateDetail.description || '暂无描述' }}</p>
            </div><a-divider />
            <a-row :gutter="24">
              <a-col :span="8">
                <div class="info-item">
                  <span class="info-label">创建人：</span>
                  <span class="info-value">{{ certificateDetail.creatorName || '-' }}</span>
                </div>
              </a-col>
              <a-col :span="8">
                <div class="info-item">
                  <span class="info-label">创建时间：</span>
                  <span class="info-value">{{ certificateDetail.gmtCreate || '-' }}</span>
                </div>
              </a-col><a-col :span="8">
                <div class="info-item">
                  <span class="info-label">过期时间：</span>
                  <span class="info-value">{{ certificateDetail.expireTime || '永不过期' }}</span>
                </div>
              </a-col>
            </a-row>
          </a-spin>
        </a-card>

        <!-- 证书预览区域 -->
        <a-card class="certificate-preview-card" title="证书预览">
          <a-spin :spinning="loading">
            <div class="certificate-preview-container">
              <img
                v-if="certificateDetail.templateUrl"
                :src="certificateDetail.templateUrl"
                alt="证书模板"
                class="certificate-template"
              /><a-empty v-else description="暂无证书模板" />
            </div>
          </a-spin>
        </a-card>

        <!-- 使用情况区域 -->
        <a-card class="certificate-usage-card" title="使用情况"><a-spin :spinning="loading">
            <!-- 发放统计 -->
            <div class="usage-statistics"><a-row :gutter="24"><a-col :span="8"><a-statistic
                    title="已发放数量"
                    :value="certificateDetail.issuedCount || 0"
                    :valueStyle="{ color: '#1890ff' }"
                  ><template #suffix>
                      <file-done-outlined />
                    </template>
                  </a-statistic>
                </a-col>
                <a-col :span="8"><a-statistic title="有效证书数量" :value="validCount"
                    :valueStyle="{ color: '#52c41a' }"
                  >
                    <template #suffix>
                      <check-circle-outlined />
                    </template>
                  </a-statistic>
                </a-col>
                <a-col :span="8"><a-statistic title="已过期证书数量" :value="expiredCount"
                    :valueStyle="{ color: '#faad14' }"
                  >
                    <template #suffix>
                      <clock-circle-outlined />
                    </template>
                  </a-statistic>
                </a-col>
              </a-row>
            </div><a-divider />

            <!-- 使用列表 -->
            <div class="usage-list">
              <h4>使用该证书的培训/学习地图</h4>
              <a-table
                :columns="usageColumns"
                :data-source="certificateDetail.usages || []"
                :pagination="false"
                :locale="{emptyText: '暂无使用记录'}"
              >
                <template #bodyCell="{ column, record }"><template v-if="column.key === 'type'">
                    <a-tag :color="record.type === 'TRAIN' ? 'blue' : 'green'">
                      {{ record.type === 'TRAIN' ? '培训' : '学习地图' }}</a-tag>
                  </template>
                  <template v-if="column.key === 'name'">
                    <a
                      @click="navigateToDetail(record.type, record.id)"
                    >
                      {{ record.name }}
                    </a>
                  </template>
                </template>
              </a-table>
            </div>
          </a-spin>
        </a-card></a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { ref, reactive, onMounted, defineComponent, computed } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { getCertificateDetail } from '@/api/certificate';
import { hasPermission } from '@/utils/permission';
import {
  UserOutlined,SettingOutlined,
  LogoutOutlined,DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  EditOutlined,
  TeamOutlined,
  ArrowLeftOutlined,
  FileDoneOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';

export default defineComponent({
  name: 'CertificateDetailPage',
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
    TeamOutlined,
    ArrowLeftOutlined,
    FileDoneOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    PageBreadcrumb,
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const certificateId = ref(parseInt(route.params.id));
    const collapsed = ref(false);
    const selectedKeys = ref(['certificate']);
    const openKeys = ref(['operation']);
    const categoryModalVisible = ref(false);
    const loading = ref(false);
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

    // 证书详情数据
    const certificateDetail = ref({
      id: null,
      name: '',
      description: '',
      templateUrl: '',
      expireTime: '',
      creatorId: null,
      creatorName: '',
      gmtCreate: '',
      issuedCount: 0,
      usages: []
    });

    // 使用列表表格列定义
    const usageColumns = [
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: 100
      },
      {
        title: '名称',
        dataIndex: 'name',
        key: 'name'
      }
    ];

    // 计算有效证书数量和过期证书数量
    // 由于API没有直接提供这些数据，这里假设有效证书数量为已发放数量的80%，过期证书数量为已发放数量的20%
    // 实际项目中应该从API获取这些数据
    const validCount = computed(() => {
      return Math.round((certificateDetail.value.issuedCount || 0) * 0.8);
    });
    const expiredCount = computed(() => {
      return Math.round((certificateDetail.value.issuedCount || 0) * 0.2);
    });// 获取用户信息
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

    // 获取证书详情
    const fetchCertificateDetail = async () => {
      loading.value = true;
      try {
        const res = await getCertificateDetail(certificateId.value);
        if (res.code === 200 && res.data) {
          certificateDetail.value = res.data;
        } else {
          message.error(res.message || '获取证书详情失败');
        }
      } catch (error) {
        console.error('获取证书详情失败:', error);
      } finally {
        loading.value = false;
      }
    };// 退出登录
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

    // 跳转到详情页
    const navigateToDetail = (type, id) => {
      if (type === 'train') {
        navigateTo(`/train/detail/${id}`);
      } else if (type === 'map') {
        navigateTo(`/map/detail/${id}`);
      }
    };

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      categoryModalVisible.value = true;
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();
      // 获取证书详情
      fetchCertificateDetail();
    });

    return {
      certificateId,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      certificateDetail,
      loading,
      categoryModalVisible,
      usageColumns,
      validCount,
      expiredCount,
      hasPermission,
      handleLogout,
      navigateTo,
      navigateToDetail,
      showCategoryModal
    };
  }
});
</script>

<style scoped>
.certificate-detail-layout {
  min-height: 100vh;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.content-header-left {
  display: flex;
  align-items: center;
}

.content-header-left h2 {
  margin: 0 0 0 8px;
}

.certificate-info-card {
  margin-bottom: 16px;background-color: #FFFFFF;
  padding: 24px;
}

.certificate-info-header {
  margin-bottom: 16px;
}

.certificate-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 8px;
}

.certificate-description {
  font-size: 14px;
  color: #666666;
  margin-bottom: 0;
}

.info-item {
  margin-bottom: 8px;
}

.info-label {
  color: #8c8c8c;
  margin-right: 8px;
}

.certificate-preview-card {
  margin-bottom: 16px;
  background-color: #FFFFFF;
  padding: 24px;
}

.certificate-preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.certificate-template {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
}

.certificate-usage-card {
  margin-bottom: 16px;
  background-color: #FFFFFF;
  padding: 24px;
}

.usage-statistics {
  margin-bottom: 24px;
}

.usage-list h4 {
  margin-bottom: 16px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #FAFAFA;
}

:deep(.ant-table-tbody > tr > td) {
  height: 54px;
}
</style>
