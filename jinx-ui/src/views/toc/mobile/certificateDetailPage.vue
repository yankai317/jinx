<template>
  <div class="certificate-detail-mobile-page">
    <!-- 主要内容区 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 证书图片 -->
        <div class="certificate-image-container">
          <img
            v-if="certificateDetail.templateUrl"
            :src="certificateDetail.templateUrl"
            alt="证书"
            class="certificate-image"
          />
          <div v-else class="no-image">
            <file-image-outlined />
            <p>暂无证书图片</p>
          </div>
        </div>

        <!-- 证书信息 -->
        <div class="certificate-info-card">
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
        </div>
      </a-spin>
    </div>

    <!-- 底部操作区 -->
    <div class="bottom-actions">
      <a-button
        type="primary"
        block
        @click="handleDownload"
        :disabled="!certificateDetail.templateUrl"
      >
        <template #icon><download-outlined /></template>
        下载证书
      </a-button>
      <a-button
        block
        @click="handleShare"
        :disabled="!certificateDetail.templateUrl"
        style="margin-top: 12px;"
      >
        <template #icon><share-alt-outlined /></template>
        分享证书
      </a-button>
    </div>

    <!-- 分享弹层 -->
    <a-drawer
      v-model:visible="shareVisible"
      title="分享证书"
      placement="bottom"
      height="70vh"
      :destroyOnClose="true"
      @close="closeShareModal"
    >
      <a-spin :spinning="shareLoading">
        <div class="share-content">
          <div class="share-image">
            <img v-if="shareInfo.qrCodeUrl" :src="shareInfo.qrCodeUrl" alt="分享二维码" />
            <div v-else class="no-qrcode">
              <qrcode-outlined />
              <p>二维码生成中...</p>
            </div>
          </div>
          <div class="share-info">
            <h3>{{ shareInfo.title }}</h3>
            <p>{{ shareInfo.description }}</p>
          </div>

          <div class="share-link">
            <a-input-group compact>
              <a-input
                v-model:value="shareInfo.shareUrl"
                readonly
                style="width: calc(100% - 80px)"
              />
              <a-button type="primary" @click="copyShareLink">复制链接</a-button>
            </a-input-group>
          </div>
        </div>
      </a-spin>
    </a-drawer>

    <!-- 底部导航栏 -->
    <MobileTabBar active="profile" />
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { getCertificateDetail, downloadCertificate } from '@/api/toc/certificate';
import { getShareContent } from '@/api/toc/share';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import {
  FileImageOutlined,
  DownloadOutlined,
  ShareAltOutlined,
  QrcodeOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CertificateDetailMobilePage',
  components: {
    MobileTabBar,
    FileImageOutlined,
    DownloadOutlined,
    ShareAltOutlined,
    QrcodeOutlined
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

    // 分享弹层相关
    const shareVisible = ref(false);
    const shareLoading = ref(false);
    const shareInfo = ref({
      title: '',
      description: '',
      coverImage: '',
      shareUrl: '',
      qrCodeUrl: ''
    });

    // 获取证书详情
    const fetchCertificateDetail = async () => {
      if (!certificateId.value) {
        message.error('证书ID不存在');
        router.push('/toc/mobile/certificates');
        return;
      }

      loading.value = true;
      try {
        const res = await getCertificateDetail(certificateId.value);
        if (res.code === 200 && res.data) {
          certificateDetail.value = res.data;
        } else {
          message.error(res.message || '获取证书详情失败');
          router.push('/toc/mobile/certificates');
        }
      } catch (error) {
        console.error('获取证书详情失败:', error);
        router.push('/toc/mobile/certificates');
      } finally {
        loading.value = false;
      }
    };

    // 获取分享信息
    const fetchShareInfo = async (id) => {
      shareLoading.value = true;
      try {
        const res = await getShareContent('certificate', id);
        if (res.code === 200 && res.data) {
          shareInfo.value = res.data;
        } else {
          message.error(res.message || '获取分享信息失败');
        }
      } catch (error) {
        console.error('获取分享信息失败:', error);
      } finally {
        shareLoading.value = false;
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
      if (!certificateDetail.value.id) {
        message.error('证书ID不存在');
        return;
      }

      try {
        const res = await downloadCertificate(certificateDetail.value.id);
        if (res.code === 200 && res.data) {
          // 移动端下载可能需要特殊处理，这里简单处理为打开新窗口
          window.open(res.data.downloadUrl, '_blank');
          message.success('证书下载成功');
        } else {
          message.error(res.message || '下载证书失败');
        }
      } catch (error) {
        console.error('下载证书失败:', error);
      }
    };

    // 处理分享证书
    const handleShare = () => {
      if (!certificateDetail.value.id) {
        message.error('证书ID不存在');
        return;
      }

      shareVisible.value = true;
      fetchShareInfo(certificateDetail.value.id);
    };

    // 关闭分享弹层
    const closeShareModal = () => {
      shareVisible.value = false;
      // 重置分享数据
      shareInfo.value = {
        title: '',
        description: '',
        coverImage: '',
        shareUrl: '',
        qrCodeUrl: ''
      };
    };

    // 复制分享链接
    const copyShareLink = () => {
      if (!shareInfo.value.shareUrl) {
        message.error('分享链接不存在');
        return;
      }

      // 复制到剪贴板
      const input = document.createElement('input');
      input.value = shareInfo.value.shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);

      message.success('分享链接已复制到剪贴板');
    };

    onMounted(() => {
      fetchCertificateDetail();
    });

    return {
      certificateDetail,
      loading,
      shareVisible,
      shareLoading,
      shareInfo,
      getStatusText,
      getStatusColor,
      handleDownload,
      handleShare,
      closeShareModal,
      copyShareLink
    };
  }
});
</script>

<style scoped>
.certificate-detail-mobile-page {
  padding-bottom: 140px;
}

.main-content {
  padding: 16px;
}

.certificate-image-container {
  margin-bottom: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  padding: 16px;
}

.certificate-image {
  width: 100%;
  object-fit: contain;
}

.no-image {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  color: #999;
}

.no-image .anticon {
  font-size: 36px;
  margin-bottom: 8px;
}

.certificate-info-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 16px;
}

.certificate-name {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
  line-height: 1.4;
}

.info-item {
  display: flex;
  margin-bottom: 12px;
}

.info-item .label {
  width: 80px;
  color: #666;
  flex-shrink: 0;
}

.info-item .value {
  flex: 1;
  word-break: break-all;
}

.certificate-description {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.certificate-description h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.certificate-description p {
  color: #666;
  line-height: 1.6;
}

.bottom-actions {
  position: fixed;
  bottom: 65px;
  left: 0;
  right: 0;
  padding: 16px;
  background-color: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

/* 分享样式 */
.share-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.share-image {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.share-image img {
  max-width: 200px;
  max-height: 200px;
}

.no-qrcode {
  width: 200px;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  color: #999;
}

.share-info {
  margin-bottom: 16px;
}

.share-info h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.share-link {
  margin-bottom: 16px;
}
</style>