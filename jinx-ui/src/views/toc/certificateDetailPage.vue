
<template>
  <div class="certificate-detail-container">
    <!-- 顶部导航栏 -->
    <div class="header">
      <div class="header-content">
        <div class="logo" @click="goToHome">
          <img src="@/assets/logo.png" alt="Logo" />
          <span>学习平台</span>
        </div>
        <div class="user-info" v-if="userInfo.userId">
          <a-dropdown>
            <a class="user-dropdown" @click.prevent>
              <a-avatar :src="userInfo.avatar || 'https://joeschmoe.io/api/v1/random'" />
              <span class="username">{{ userInfo.nickname }}</span>
              <down-outlined />
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="goToProfile">
                  <user-outlined />个人中心
                </a-menu-item>
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined />退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 返回按钮 -->
        <div class="back-button">
          <a-button type="link" @click="goBack">
            <left-outlined />返回
          </a-button>
        </div>

        <!-- 证书详情 -->
        <div class="certificate-detail">
          <a-row :gutter="24">
            <!-- 左侧证书图片 -->
            <a-col :span="16">
              <a-card :bordered="false" class="certificate-image-card">
                <div class="certificate-image">
                  <img v-if="certificateDetail.templateUrl" :src="certificateDetail.templateUrl" alt="证书" />
                  <div v-else class="no-image">
                    <file-image-outlined />
                    <p>暂无证书图片</p>
                  </div>
                </div>
              </a-card>
            </a-col>

            <!-- 右侧证书信息 -->
            <a-col :span="8">
              <a-card :bordered="false" title="证书信息" class="certificate-info-card">
                <div class="certificate-info">
                  <h2 class="certificate-name">{{ certificateDetail.name }}</h2>

                  <div class="info-item">
                    <span class="label">证书编号：</span>
                    <span class="value">{{ certificateDetail.certificateNo }}</span>
                  </div>

                  <div class="info-item">
                    <span class="label">颁发时间：</span>
                    <span class="value">{{ certificateDetail.issueTime }}</span>
                  </div>

                  <div class="info-item">
                    <span class="label">有效期至：</span>
                    <span class="value">{{ certificateDetail.expireTime || '永久有效' }}</span>
                  </div>

                  <div class="info-item">
                    <span class="label">证书来源：</span>
                    <span class="value">{{ certificateDetail.sourceName || certificateDetail.sourceType }}</span>
                  </div>

                  <div class="info-item">
                    <span class="label">证书状态：</span>
                    <a-tag :color="getStatusColor(certificateDetail.status)">
                      {{ getStatusText(certificateDetail.status) }}
                    </a-tag>
                  </div>

                  <div class="certificate-description" v-if="certificateDetail.description">
                    <h3>证书说明</h3>
                    <p>{{ certificateDetail.description }}</p>
                  </div>

                  <div class="certificate-actions">
                    <a-button type="primary" @click="handleDownload" :disabled="!certificateDetail.templateUrl">
                      <download-outlined />下载证书
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { getCertificateDetail, downloadCertificate } from '@/api/toc/certificate';
import {
  UserOutlined,
  DownOutlined,
  LogoutOutlined,
  LeftOutlined,
  FileImageOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CertificateDetailPage',
  components: {
    UserOutlined,
    DownOutlined,
    LogoutOutlined,
    LeftOutlined,
    FileImageOutlined,
    DownloadOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const certificateId = ref(parseInt(route.params.id) || 0);
    const loading = ref(false);

    // 证书详情
    const certificateDetail = ref({
      id: 0,
      certificateId: 0,
      name: '',
      certificateNo: '',
      templateUrl: '',
      sourceType: '',
      sourceName: '',
      issueTime: '',
      expireTime: '',
      status: 0,
      description: ''
    });

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: ''
    });

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

    // 获取证书详情
    const fetchCertificateDetail = async () => {
      if (!certificateId.value) {
        message.error('证书ID不存在');
        router.push('/toc/certificates');
        return;
      }

      loading.value = true;
      try {
        const res = await getCertificateDetail(certificateId.value);
        if (res.code === 200 && res.data) {
          certificateDetail.value = res.data;
        } else {
          message.error(res.message || '获取证书详情失败');
          router.push('/toc/certificates');
        }
      } catch (error) {
        console.error('获取证书详情失败:', error);
        router.push('/toc/certificates');
      } finally {
        loading.value = false;
      }
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        0: '有效',
        1: '已过期',
        2: '已撤销'
      };
      return statusMap[status] || '未知';
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const colorMap = {
        0: 'success',
        1: 'warning',
        2: 'error'
      };
      return colorMap[status] || 'default';
    };

    // 处理下载证书
    const handleDownload = async () => {
      if (!certificateId.value) {
        message.error('证书ID不存在');
        return;
      }

      loading.value = true;
      try {
        const res = await downloadCertificate(certificateId.value);
        if (res.code === 200 && res.data) {
          // 打开下载链接
          window.open(res.data, '_blank');
        } else {
          message.error(res.message || '下载证书失败');
        }
      } catch (error) {
        console.error('下载证书失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 返回上一页
    const goBack = () => {
      router.go(-1);
    };

    // 跳转到首页
    const goToHome = () => {
      router.push('/');
    };

    // 跳转到个人中心
    const goToProfile = () => {
      router.push('/personal/center');
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
      getUserInfo();
      // 获取证书详情
      fetchCertificateDetail();
    });

    return {
      certificateId,
      loading,
      certificateDetail,
      userInfo,
      getStatusText,
      getStatusColor,
      handleDownload,
      goBack,
      goToHome,
      goToProfile,
      handleLogout
    };
  }
});
</script>

<style scoped>
.certificate-detail-container {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  max-width: 1200px;
  margin: 0 auto;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.logo img {
  height: 32px;
  margin-right: 8px;
}

.logo span {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin: 0 8px;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.back-button {
  margin-bottom: 16px;
}

.certificate-detail {
  margin-bottom: 24px;
}

.certificate-image-card {
  margin-bottom: 24px;
}

.certificate-image {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  min-height: 400px;
}

.certificate-image img {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.no-image {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: rgba(0, 0, 0, 0.25);
}

.no-image .anticon {
  font-size: 64px;
  margin-bottom: 16px;
}

.certificate-info-card {
  height: 100%;
}

.certificate-name {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1890ff;
}

.info-item {
  margin-bottom: 16px;
  display: flex;
}

.label {
  color: rgba(0, 0, 0, 0.45);
  min-width: 80px;
}

.value {
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.certificate-description {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.certificate-description h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.certificate-description p {
  color: rgba(0, 0, 0, 0.65);
  white-space: pre-wrap;
}

.certificate-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
