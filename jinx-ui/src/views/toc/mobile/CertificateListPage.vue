<template>
  <div class="certificate-list-mobile-page">
    <!-- 主要内容区 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 页面标题 -->
        <div class="page-header">
          <div class="page-description">查看您获得的所有证书</div>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-section">
          <a-radio-group v-model:value="status" @change="handleStatusChange" buttonStyle="solid" size="small">
            <a-radio-button :value="0">全部</a-radio-button>
            <a-radio-button :value="1">有效</a-radio-button>
            <a-radio-button :value="2">已过期</a-radio-button>
            <a-radio-button :value="3">已撤销</a-radio-button>
          </a-radio-group>
        </div>

        <!-- 证书列表 -->
        <div class="certificate-list">
          <EmptyState
            v-if="certificateList.length === 0"
            title="暂无证书"
            description="您目前还没有获得任何证书，完成学习任务可获得证书"
          >
            <template #icon>
              <trophy-outlined />
            </template>
            <template #action>
              <a-button type="primary" @click="goToLearningCenter">
                去学习中心
              </a-button>
            </template>
          </EmptyState>

          <div v-else class="certificate-items">
            <div
              v-for="certificate in certificateList"
              :key="certificate.id"
              class="certificate-item"
              @click="viewCertificateDetail(certificate)"
            >
              <div class="certificate-image">
                <img v-if="certificate.templateUrl" :src="certificate.templateUrl" alt="证书模板" />
                <div v-else class="no-image">
                  <file-image-outlined />
                  <p>暂无证书图片</p>
                </div>
              </div>
              <div class="certificate-info">
                <h3 class="certificate-name">{{ certificate.name }}</h3>
                <p class="certificate-no">证书编号: {{ certificate.certificateNo }}</p>
                <p class="certificate-source">来源: {{ certificate.sourceName || certificate.sourceType }}</p>
                <p class="certificate-date">颁发时间: {{ certificate.issueTime }}</p>
                <p class="certificate-date">
                  有效期至: {{ certificate.expireTime || '永久有效' }}
                </p>
                <a-tag :color="getStatusColor(certificate.status)" class="certificate-status">
                  {{ getStatusText(certificate.status) }}
                </a-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <a-pagination
            v-model:current="pageNum"
            v-model:pageSize="pageSize"
            :total="total"
            :showTotal="total => `共 ${total} 条记录`"
            simple
            @change="handlePageChange"
          />
        </div>
      </a-spin>
    </div>

    <!-- 证书详情弹层 -->
    <a-drawer
      v-model:visible="detailVisible"
      :title="selectedCertificate.name"
      placement="bottom"
      height="85vh"
      :destroyOnClose="true"
      @close="closeDetailModal"
    >
      <a-spin :spinning="detailLoading">
        <div class="certificate-detail">
          <div class="certificate-image-container">
            <img
              v-if="certificateDetail.templateUrl"
              :src="certificateDetail.templateUrl"
              alt="证书"
              class="certificate-detail-image"
            />
            <div v-else class="no-detail-image">
              <file-image-outlined />
              <p>暂无证书图片</p>
            </div>
          </div>

          <div class="certificate-detail-info">
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

          <div class="certificate-actions">
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
        </div>
      </a-spin>
    </a-drawer>

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
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { getUserCertificates, getCertificateDetail, downloadCertificate } from '@/api/toc/certificate';
import { getShareContent } from '@/api/toc/share';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import {
  FileImageOutlined,
  DownloadOutlined,
  ShareAltOutlined,
  QrcodeOutlined,
  TrophyOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CertificateListMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    FileImageOutlined,
    DownloadOutlined,
    ShareAltOutlined,
    QrcodeOutlined,
    TrophyOutlined
  },
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const certificateList = ref([]);
    const total = ref(0);
    const pageNum = ref(1);
    const pageSize = ref(10);
    const status = ref(0); // 0-全部，1-有效，2-已过期，3-已撤销

    // 详情弹层相关
    const detailVisible = ref(false);
    const detailLoading = ref(false);
    const selectedCertificate = ref({});
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

    // 获取证书列表
    const fetchCertificateList = async () => {
      loading.value = true;
      try {
        const res = await getUserCertificates({
          status: status.value,
          pageNum: pageNum.value,
          pageSize: pageSize.value
        });

        if (res.code === 200 && res.data) {
          certificateList.value = res.data.list || [];
          total.value = res.data.total || 0;
        } else {
          message.error(res.message || '获取证书列表失败');
        }
      } catch (error) {
        console.error('获取证书列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取证书详情
    const fetchCertificateDetail = async (id) => {
      detailLoading.value = true;
      try {
        const res = await getCertificateDetail(id);
        if (res.code === 200 && res.data) {
          certificateDetail.value = res.data;
        } else {
          message.error(res.message || '获取证书详情失败');
        }
      } catch (error) {
        console.error('获取证书详情失败:', error);
      } finally {
        detailLoading.value = false;
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

    // 处理状态变更
    const handleStatusChange = () => {
      pageNum.value = 1; // 重置页码
      fetchCertificateList();
    };

    // 处理页码变更
    const handlePageChange = (page) => {
      pageNum.value = page;
      fetchCertificateList();
    };

    // 查看证书详情
    const viewCertificateDetail = (certificate) => {
      selectedCertificate.value = certificate;
      detailVisible.value = true;
      fetchCertificateDetail(certificate.id);
    };

    // 关闭详情弹层
    const closeDetailModal = () => {
      detailVisible.value = false;
      // 重置详情数据
      certificateDetail.value = {
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
      };
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

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/mobile/learning/center');
    };

    onMounted(() => {
      fetchCertificateList();
    });

    return {
      loading,
      certificateList,
      total,
      pageNum,
      pageSize,
      status,
      detailVisible,
      detailLoading,
      selectedCertificate,
      certificateDetail,
      shareVisible,
      shareLoading,
      shareInfo,
      getStatusText,
      getStatusColor,
      handleStatusChange,
      handlePageChange,
      viewCertificateDetail,
      closeDetailModal,
      handleDownload,
      handleShare,
      closeShareModal,
      copyShareLink,
      goToLearningCenter
    };
  }
});
</script>

<style scoped>
.certificate-list-mobile-page {
  padding-bottom: 70px;
}

.main-content {
  padding: 16px;
}

.page-header {
  margin-bottom: 16px;
}

.page-description {
  color: #666;
  font-size: 14px;
}

.filter-section {
  margin-bottom: 16px;
  overflow-x: auto;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 8px;
}

.certificate-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.certificate-item {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.certificate-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
  position: relative;
}

.certificate-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
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

.certificate-info {
  padding: 12px;
}

.certificate-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.certificate-no,
.certificate-source,
.certificate-date {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.certificate-status {
  margin-top: 8px;
}

.pagination-container {
  margin-top: 24px;
  text-align: center;
}

/* 证书详情样式 */
.certificate-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.certificate-image-container {
  width: 100%;
  margin-bottom: 16px;
  text-align: center;
}

.certificate-detail-image {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
}

.no-detail-image {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  color: #999;
}

.certificate-detail-info {
  flex: 1;
  margin-bottom: 16px;
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

.certificate-actions {
  margin-top: auto;
  padding-top: 16px;
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
