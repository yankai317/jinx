<template>
  <a-layout class="certificate-user-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <h2>证书获得学员</h2>
          <div class="content-header-right">
            <a-button @click="navigateTo('/certificate/list')">
              <arrow-left-outlined />返回列表
            </a-button>
          </div>
        </div>

        <!-- 证书信息卡片 -->
        <a-card class="certificate-card" v-if="certificateDetail">
          <div class="certificate-info">
            <div class="certificate-image">
              <img :src="certificateDetail.templateUrl || 'https://via.placeholder.com/120'" alt="证书模板" />
            </div>
            <div class="certificate-detail">
              <h3>{{ certificateDetail.name }}</h3>
              <p>{{ certificateDetail.description }}</p>
              <div class="certificate-meta">
                <span>创建人: {{ certificateDetail.creatorName }}</span>
                <span>创建时间: {{ certificateDetail.gmtCreate }}</span>
                <span>过期时间: {{ certificateDetail.expireTime || '永久有效' }}</span>
                <span>发放数量: {{ certificateDetail.issuedCount || 0 }}</span>
              </div>
            </div>
          </div>
        </a-card>

        <!-- 搜索和筛选区域 -->
        <a-card class="search-card">
          <a-form layout="inline" :model="searchForm">
            <a-row :gutter="16" style="width: 100%" :label-col="{ style: { width: '80px' } }"
              :wrapperCol="{ style: { flex: 1 } }">
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="姓名/工号">
                  <a-input v-model:value="searchForm.keyword" placeholder="请输入姓名或工号" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="来源类型">
                  <a-select v-model:value="searchForm.sourceType" placeholder="请选择来源类型" allowClear>
                    <a-select-option value="ALL">全部</a-select-option>
                    <a-select-option value="TRAIN">培训</a-select-option>
                    <a-select-option value="LEARNING_MAP">学习地图</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 24 }" :xl="{ span: 8 }" :xxl="{ span: 12 }"
                style="text-align: right">
                <a-form-item>
                  <a-space>
                    <a-button type="primary" @click="handleSearch">
                      <search-outlined />搜索
                    </a-button>
                    <a-button @click="handleReset">
                      <reload-outlined />重置</a-button>
                  </a-space>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>

        <!-- 用户列表表格 -->
        <a-card class="table-card">
          <!-- 操作按钮区 -->
          <a-row style="margin-bottom: 16px">
            <a-col :span="24" style="text-align: right">
              <a-space>
                <a-button type="primary" @click="handleExport" :disabled="userList.length === 0">
                  <download-outlined />导出
                </a-button>
                <!-- <a-button danger @click="handleBatchRevoke"
                  :disabled="selectedRowKeys.length === 0 || !hasPermission('certificate:revoke')">
                  <stop-outlined />批量撤销
                </a-button> -->
              </a-space>
            </a-col>
          </a-row>
          <a-table :columns="columns" :data-source="userList" :pagination="pagination" :loading="loading"
            :row-key="record => record.id"
            @change="handleTableChange"
            :scroll="{ x: '100%' }">
          <!-- <a-table :columns="columns" :data-source="userList" :pagination="pagination" :loading="loading"
            :row-key="record => record.id"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
            @change="handleTableChange"
            :scroll="{ x: '100%' }"></a-table> -->
            <!-- 状态列 -->
            <template #bodyCell="{ column, record }">
              <!-- 来源列 -->
              <template v-if="column.key === 'sourceType'">
                <a-tag :color="getSourceColor(record.sourceType)">
                  {{ getSourceText(record.sourceType) }}
                </a-tag>
              </template>

              <!-- 操作列 -->
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <!-- 查看证书 -->
                  <a-tooltip>
                    <template #title>查看证书</template>
                    <a @click="handleViewCertificate(record)">
                      <eye-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 撤销证书 -->
                  <a-tooltip v-if="hasPermission('certificate:revoke') && record.status === 'valid'">
                    <template #title>撤销证书</template>
                    <a-popconfirm title="确定要撤销此证书吗？" ok-text="确定" cancel-text="取消" @confirm="handleRevoke(record)">
                      <a class="danger-link"><stop-outlined /></a>
                    </a-popconfirm>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-layout-content>
    </a-layout>

    <!-- 查看证书弹窗 -->
    <a-modal v-model:visible="viewCertificateModalVisible" title="查看证书" width="800px" :footer="null" destroyOnClose>
      <div v-if="viewCertificateModalVisible && currentUserCertificate" class="certificate-preview">
        <div class="certificate-preview-image">
          <img :src="certificateDetail?.templateUrl || 'https://via.placeholder.com/600x400'" alt="证书" />
        </div>
        <div class="certificate-preview-info">
          <h3>{{ certificateDetail?.name }}</h3>
          <p>{{ certificateDetail?.description }}</p>
          <div class="certificate-preview-meta">
            <p><strong>获得者:</strong> {{ currentUserCertificate.userName }}</p>
            <p><strong>获得时间:</strong> {{ currentUserCertificate.issueTime }}</p>
            <p><strong>过期时间:</strong> {{ currentUserCertificate.expireTime || '永久有效' }}</p>
            <p><strong>状态:</strong> <a-tag :color="getStatusColor(currentUserCertificate.status)">{{
              getStatusText(currentUserCertificate.status) }}</a-tag></p>
            <p v-if="currentUserCertificate.status === 'revoked'"><strong>撤销原因:</strong> {{
              currentUserCertificate.revokeReason }}</p>
            <p><strong>来源:</strong> <a-tag :color="getSourceColor(currentUserCertificate.sourceType)">{{
              getSourceText(currentUserCertificate.sourceType) }}</a-tag></p>
            <p v-if="currentUserCertificate.sourceName"><strong>来源名称:</strong> {{ currentUserCertificate.sourceName }}
            </p>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 撤销证书弹窗 -->
    <a-modal v-model:visible="revokeModalVisible" title="撤销证书" @ok="handleRevokeConfirm"
      @cancel="revokeModalVisible = false" :confirmLoading="revokeLoading">
      <a-form :model="revokeForm" layout="vertical">
        <a-form-item label="撤销原因" name="reason" :rules="[{ required: true, message: '请输入撤销原因' }]">
          <a-textarea v-model:value="revokeForm.reason" placeholder="请输入撤销原因" :rows="4" :maxLength="200" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script>
import { ref, reactive, onMounted, defineComponent } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { getCertificateDetail, getCertificateUsers, revokeCertificate } from '@/api/certificate';
import { hasPermission } from '@/utils/permission';
import {
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
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  StopOutlined,
  ArrowLeftOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'CertificateUserPage',
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
    SearchOutlined,
    ReloadOutlined,
    EyeOutlined,
    StopOutlined,
    ArrowLeftOutlined,
    DownloadOutlined,
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const route = useRoute();
    const certificateId = ref(parseInt(route.params.id));
    const collapsed = ref(false);
    const selectedKeys = ref(['certificate']);
    const openKeys = ref(['operation']);
    const categoryModalVisible = ref(false);
    const viewCertificateModalVisible = ref(false);
    const revokeModalVisible = ref(false);
    const revokeLoading = ref(false);
    const loading = ref(true);
    const selectedRowKeys = ref([]);

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

    // 证书详情
    const certificateDetail = ref(null);

    // 用户列表数据
    const userList = ref([]);

    // 当前选中的用户证书
    const currentUserCertificate = ref(null);

    // 搜索表单
    const searchForm = reactive({
      keyword: '',
      status: 'ALL',
      sourceType: 'ALL',
      pageNum: 1,
      pageSize: 10
    });

    // 撤销表单
    const revokeForm = reactive({
      userCertificateId: null,
      userCertificateIds: null,
      reason: '',
      isBatch: false
    });

    // 分页配置
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 表格列定义
    const columns = [
      {
        title: '姓名',
        dataIndex: 'userName',
        key: 'userName',
        width: 100
      },
      {
        title: '工号',
        dataIndex: 'employeeNo',
        key: 'employeeNo',
        width: 120
      },
      {
        title: '来源类型',
        dataIndex: 'sourceType',
        key: 'sourceType',
        width: 100
      },
      {
        title: '来源名称',
        dataIndex: 'sourceName',
        key: 'sourceName',
        ellipsis: true,
        width: 160
      },
      {
        title: '获得时间',
        dataIndex: 'issueTime',
        key: 'issueTime',
        width: 200
      },
      {
        title: '过期时间',
        dataIndex: 'expireTime',
        key: 'expireTime',
        width: 200
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 60
      }
    ];

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
      try {
        const res = await getCertificateDetail(certificateId.value);
        if (res.code === 200 && res.data) {
          certificateDetail.value = res.data;
        } else {
          message.error(res.message || '获取证书详情失败');
          router.push('/certificate/list');
        }
      } catch (error) {
        console.error('获取证书详情失败:', error);
        router.push('/certificate/list');
      }
    };

    // 获取证书用户列表
    const fetchCertificateUsers = async () => {
      loading.value = true;
       const data = {
        ...searchForm,
        status: searchForm.status === 'ALL' ? undefined : searchForm.status,
        sourceType: searchForm.sourceType === 'ALL' ? undefined : searchForm.sourceType
       }
      try {
        const res = await getCertificateUsers(certificateId.value, data);
        if (res.code === 200 && res.data) {
          userList.value = res.data.list || [];
          pagination.total = res.data.total || 0;
          pagination.current = searchForm.pageNum;
          pagination.pageSize = searchForm.pageSize;
        } else {
          message.error(res.message || '获取证书用户列表失败');
        }
      } catch (error) {
        console.error('获取证书用户列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理搜索
    const handleSearch = () => {
      searchForm.pageNum = 1;
      fetchCertificateUsers();
    };

    // 处理重置
    const handleReset = () => {
      // 重置搜索表单
      searchForm.keyword = '';
      searchForm.status = 'ALL';
      searchForm.sourceType = 'ALL';
      searchForm.pageNum = 1;

      // 重新获取数据
      fetchCertificateUsers();
    };

    // 处理表格变更
    const handleTableChange = (pag) => {
      // 处理分页
      searchForm.pageNum = pag.current;
      searchForm.pageSize = pag.pageSize;

      // 重新获取数据
      fetchCertificateUsers();
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        0: '有效',
        1: '已过期',
        2: '已撤销'
      };
      return statusMap[status] || status;
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const colorMap = {
        0: 'green',
        1: 'orange',
        2: 'red'
      };
      return colorMap[status] || 'default';
    };

    // 获取来源文本
    const getSourceText = (sourceType) => {
      const sourceMap = {
        TRAIN: '培训',
        LEARNING_MAP: '学习地图',
        MAP_STAGE: '地图阶段'
      };
      return sourceMap[sourceType] || sourceType;
    };

    // 获取来源颜色
    const getSourceColor = (sourceType) => {
      const colorMap = {
        TRAIN: 'blue',
        LEARNING_MAP: 'purple',
        MAP_STAGE: 'orange'
      };
      return colorMap[sourceType] || 'default';
    };

    // 查看证书
    const handleViewCertificate = (record) => {
      currentUserCertificate.value = record;
      viewCertificateModalVisible.value = true;
    };

    // 选择行变更
    const onSelectChange = (keys) => {
      selectedRowKeys.value = keys;
    };

    // 导出证书用户列表
    const handleExport = () => {
      // 创建一个隐藏的a标签用于下载
      const element = document.createElement('a');

      // 构建导出URL，包含当前筛选条件
      const params = new URLSearchParams();
      if (searchForm.keyword) params.append('keyword', searchForm.keyword);
      if (searchForm.status !== 'ALL') params.append('status', searchForm.status);
      if (searchForm.sourceType !== 'ALL') params.append('sourceType', searchForm.sourceType);

      const exportUrl = `/api/certificate/export/${certificateId.value}?${params.toString()}`;

      element.setAttribute('href', exportUrl);
      element.setAttribute('download', `证书获得者列表_${certificateDetail.value?.name || '导出'}_${dayjs().format('YYYY-MM-DD')}.xlsx`);

      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);

      message.success('导出成功');
    };

    // 批量撤销证书
    const handleBatchRevoke = () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要撤销的证书');
        return;
      }

      // 设置批量撤销
      revokeForm.userCertificateIds = selectedRowKeys.value;
      revokeForm.reason = '';
      revokeForm.isBatch = true;
      revokeModalVisible.value = true;
    };

    // 撤销证书
    const handleRevoke = (record) => {
      currentUserCertificate.value = record;
      revokeForm.userCertificateId = record.id;
      revokeForm.userCertificateIds = null;
      revokeForm.reason = '';
      revokeForm.isBatch = false;
      revokeModalVisible.value = true;
    };

    // 确认撤销证书
    const handleRevokeConfirm = async () => {
      if (!revokeForm.reason) {
        message.warning('请输入撤销原因');
        return;
      }

      revokeLoading.value = true;
      try {
        let res;

        if (revokeForm.isBatch) {
          // 批量撤销
          res = await batchRevokeCertificate(certificateId.value, {
            userCertificateIds: revokeForm.userCertificateIds,
            reason: revokeForm.reason
          });
        } else {
          // 单个撤销
          res = await revokeCertificate(certificateId.value, {
            userCertificateId: revokeForm.userCertificateId,
            reason: revokeForm.reason
          });
        }

        if (res.code === 200) {
          message.success(revokeForm.isBatch ? '批量撤销证书成功' : '撤销证书成功');
          revokeModalVisible.value = false;

          // 清空选中的行
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchCertificateUsers();
          fetchCertificateDetail();
        } else {
          message.error(res.message || '撤销证书失败');
        }
      } catch (error) {
        console.error('撤销证书失败:', error);
      } finally {
        revokeLoading.value = false;
      }
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

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      categoryModalVisible.value = true;
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();

      // 获取证书详情
      fetchCertificateDetail();

      // 获取证书用户列表
      fetchCertificateUsers();
    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      certificateDetail,
      userList,
      loading,
      searchForm,
      pagination,
      columns,
      categoryModalVisible,
      viewCertificateModalVisible,
      revokeModalVisible,
      revokeLoading,
      revokeForm,
      currentUserCertificate,
      hasPermission,
      selectedRowKeys,
      handleSearch,
      handleReset,
      handleTableChange,
      getStatusText,
      getStatusColor,
      getSourceText,
      getSourceColor,
      handleViewCertificate,
      handleRevoke,
      handleBatchRevoke,
      handleRevokeConfirm,
      handleExport,
      onSelectChange,
      handleLogout,
      navigateTo,
      showCategoryModal
    };
  }
});
</script>

<style scoped>
.certificate-user-layout {
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

.content-header h2 {
  margin: 0;
}

.certificate-card {
  margin-bottom: 16px;
}

.certificate-info {
  display: flex;
  align-items: flex-start;
}

.certificate-image {
  margin-right: 24px;
}

.certificate-image img {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
}

.certificate-detail {
  flex: 1;
}

.certificate-detail h3 {
  margin-top: 0;
  margin-bottom: 8px;
}

.certificate-detail p {
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.65);
}

.certificate-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: rgba(0, 0, 0, 0.45);
}

.search-card {
  margin-bottom: 16px;
}

.action-card {
  margin-bottom: 16px;
}

.table-card {
  background-color: #fff;
}

.danger-link {
  color: #ff4d4f;
}

.danger-link:hover {
  color: #ff7875;
}

.certificate-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.certificate-preview-image {
  margin-bottom: 24px;
  text-align: center;
}

.certificate-preview-image img {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: 4px;
}

.certificate-preview-info {
  width: 100%;
}

.certificate-preview-info h3 {
  margin-top: 0;
  margin-bottom: 8px;
  text-align: center;
}

.certificate-preview-info p {
  margin-bottom: 8px;
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
}

.certificate-preview-meta {
  margin-top: 16px;
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.certificate-preview-meta p {
  margin-bottom: 8px;
  text-align: left;
}
</style>
