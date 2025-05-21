
<template>
  <div class="certificate-list-container">
    <!-- 顶部导航区域 -->
    <HeaderComponent activeKey="PERSONAL_CENTER"  />

    <!-- 主要内容区 -->
    <div class="main-content">
      <a-spin :spinning="loading">
        <!-- 页面标题 -->
        <div class="page-header">
          <h1 class="page-title">我的证书</h1>
          <div class="page-description">查看您获得的所有证书</div>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-section">
          <a-radio-group v-model:value="status" @change="handleStatusChange" buttonStyle="solid">
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
          <a-row :gutter="[24, 24]" v-else>
            <a-col :xs="24" :sm="12" :md="8" v-for="certificate in certificateList" :key="certificate.id">
              <a-card hoverable class="certificate-card" @click="viewCertificateDetail(certificate)">
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
                  <p class="certificate-source">来源类型: {{ certificate.sourceTypeName }}</p>
                  <p class="certificate-source">来源标题: {{ certificate.sourceName }}</p>
                  <p class="certificate-date">颁发时间: {{ certificate.issueTime }}</p>
                  <p class="certificate-date">
<!--                    有效期至: {{ certificate.expireTime || '永久有效' }}-->
                  </p>
                  <a-tag :color="getStatusColor(certificate.status)" class="certificate-status">
                    {{ getStatusText(certificate.status) }}
                  </a-tag>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <a-pagination
            v-model:current="pageNum"
            v-model:pageSize="pageSize"
            :total="total"
            :showTotal="total => `共 ${total} 条记录`"
            :pageSizeOptions="['10', '20', '50']"
            showSizeChanger
            @change="handlePageChange"
            @showSizeChange="handlePageSizeChange"
          />
        </div>
      </a-spin>
    </div>

    <!-- 证书详情弹层 -->
    <a-modal
      v-model:visible="detailVisible"
      :title="selectedCertificate.name"
      width="800px"
      :footer="null"
      :destroyOnClose="true"
      @cancel="closeDetailModal"
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
            </div><div class="info-item">
              <span class="label">证书状态：</span><a-tag :color="getStatusColor(certificateDetail.status)">
                {{ getStatusText(certificateDetail.status) }}
              </a-tag>
            </div><div class="certificate-description" v-if="certificateDetail.description">
              <h3>证书说明</h3>
              <p>{{ certificateDetail.description }}</p>
            </div>
          </div>

          <div class="certificate-actions">
            <a-button
              type="primary"
              @click="handleDownload"
              :disabled="!certificateDetail.templateUrl"
            >
              <template #icon><download-outlined /></template>
              下载证书
            </a-button>
            <a-button
              @click="handleShare"
              :disabled="!certificateDetail.templateUrl"
            >
              <template #icon><share-alt-outlined /></template>
              分享证书
            </a-button>
          </div>
        </div>
      </a-spin>
    </a-modal>

    <!-- 分享弹层 -->
    <a-modal
      v-model:visible="shareVisible"
      title="分享证书"
      :footer="null"
      @cancel="closeShareModal"
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

          <div class="share-actions">
            <a-button @click="closeShareModal">关闭</a-button>
          </div>
        </div>
      </a-spin>
    </a-modal>

    <!-- 底部区域 -->
    <FooterComponent />
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { getUserCertificates, getCertificateDetail, downloadCertificate } from '@/api/toc/certificate';
import { getShareContent } from '@/api/toc/share';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import {
  FileImageOutlined,
  DownloadOutlined,
  ShareAltOutlined,
  QrcodeOutlined,
  TrophyOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CertificateListPage',
  components: {
    HeaderComponent,
    FooterComponent,
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
    const handlePageChange = (page, pageSize) => {
      pageNum.value = page;
      fetchCertificateList();
    };

    // 处理每页条数变更
    const handlePageSizeChange = (current, size) => {
      pageSize.value = size;
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

      detailLoading.value = true;
      try {
        const res = await downloadCertificate(certificateDetail.value.id);
        if (res.code === 200 && res.data) {
          // 打开下载链接
          window.open(res.data, '_blank');
        } else {
          message.error(res.message || '下载证书失败');
        }
      } catch (error) {
        console.error('下载证书失败:', error);
      } finally {
        detailLoading.value = false;
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
      const input = document.createElement('input');
      input.value = shareInfo.value.shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      message.success('链接已复制到剪贴板');
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/learning/center');
    };

    onMounted(() => {
      // 获取证书列表
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
      handlePageSizeChange,
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
.certificate-list-container {
  min-height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1f1f1f;
}

.page-description {
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;
}

.filter-section {
  margin-bottom: 24px;
  background-color: #fff;
  padding: 16px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.certificate-list {
  margin-bottom: 24px;
}

.certificate-card {
  height: 100%;
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
}

.certificate-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.certificate-image {
  height: 160px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 16px;
}

.certificate-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: rgba(0, 0, 0, 0.25);
  height: 100%;
}

.no-image .anticon {
  font-size: 48px;
  margin-bottom: 8px;
}

.certificate-info {
  padding: 0 8px;
}

.certificate-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f1f1f;
}

.certificate-no,
.certificate-source,
.certificate-date {
  margin-bottom: 4px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.certificate-status {
  margin-top: 8px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

/* 证书详情弹层样式 */
.certificate-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.certificate-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  min-height: 300px;
}

.certificate-detail-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.no-detail-image {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  color: rgba(0, 0, 0, 0.25);
}

.no-detail-image .anticon {
  font-size: 64px;
  margin-bottom: 16px;
}

.certificate-detail-info {
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.info-item {
  margin-bottom: 12px;
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
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.certificate-description h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #1f1f1f;
}

.certificate-description p {
  color: rgba(0, 0, 0, 0.65);
  white-space: pre-wrap;
}

.certificate-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 分享弹层样式 */
.share-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.share-image {
  width: 200px;
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.share-image img {
  max-width: 100%;
  max-height: 100%;
}

.no-qrcode {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: rgba(0, 0, 0, 0.25);
}

.no-qrcode .anticon {
  font-size: 48px;
  margin-bottom: 8px;
}

.share-info {
  text-align: center;
  margin-bottom: 16px;
}

.share-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1f1f1f;
}

.share-info p {
  color: rgba(0, 0, 0, 0.65);
}

.share-link {
  width: 100%;
  margin-bottom: 16px;
}

.share-actions {
  display: flex;
  justify-content: center;
}
</style>
