<template>
  <a-layout class="certificate-list-layout">
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
        <div class="content-header">
          <PageBreadcrumb />
          <div class="content-header-right">
            <a-button
              v-if="hasPermission('certificate:create')"
              type="primary"
              @click="navigateTo('/certificate/create')"
            >
              <plus-outlined />创建证书
            </a-button>
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <a-card class="search-card">
          <a-form layout="inline" :model="searchForm" :label-col="{ style: { width: '80px' } }"
          :wrapperCol="{ style: { flex: 1 } }">
            <a-row :gutter="16" style="width: 100%">
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="证书名称">
                  <a-input
                    v-model:value="searchForm.name"
                    placeholder="请输入证书名称"
                    allowClear
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建人">
                  <UserSelect v-model:value="searchForm.creatorId" :show-search="true" placeholder="请选择创建人" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建时间">
                  <a-range-picker
                    v-model:value="dateRange"
                    style="width: 100%"
                    @change="handleDateRangeChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 24 }" :xl="{ span: 24 }" :xxl="{ span: 24 }"  style="display: flex;justify-content: flex-end">
                <a-form-item>
                  <a-checkbox v-model:checked="searchForm.onlyMine">只看我创建的</a-checkbox>
                  <a-space>
                    <a-button type="primary" @click="handleSearch">
                      <search-outlined />搜索
                    </a-button>
                    <a-button @click="handleReset">
                      <reload-outlined />重置
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>

        <!-- 表格操作区域 -->
        <a-card class="table-card">
          <div class="table-operations">
            <a-space>
              <a-button
                v-if="hasPermission('certificate:batchDelete')"
                type="danger"
                :disabled="selectedRowKeys.length === 0"
                @click="handleBatchDelete"
              >
                <delete-outlined />批量删除
              </a-button>
            </a-space>
          </div>

          <!-- 证书列表表格 -->
          <a-table
            :columns="columns"
            :data-source="certificateList"
            :pagination="pagination"
            :loading="loading"
            :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
            :row-key="record => record.id"
            :scroll="{ x: '100%' }"
            @change="handleTableChange"
          >
            <!-- 证书模板列 -->
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'templateUrl'">
                <a-image
                  :width="40"
                  :src="record.templateUrl || 'https://via.placeholder.com/40'"
                  :preview="false"
                />
              </template>

              <!-- 证书名称列 -->
              <template v-if="column.key === 'name'">
                <a @click="navigateTo(`/certificate/detail/${record.id}`)">{{ record.name }}</a>
              </template>

              <!-- 过期时间列 -->
              <template v-if="column.key === 'expireTime'">
                <span>{{ record.expireTime || '-' }}</span>
              </template>

              <!-- 状态列 -->
              <template v-if="column.key === 'status'">
<!--                <a-tag :color="record.status === 'published' ? 'green' : 'orange'">-->
                <a-tag :color="'published' === 'published' ? 'green' : 'orange'">
                  {{ record.status === 'published' ? '已启用' : '已禁用' }}
                </a-tag>
              </template>

              <!-- 操作列 -->
              <template v-if="column.key === 'action'">
                <a-space wrap>

                  <!-- 编辑 -->
                  <a-tooltip v-if="hasPermission('certificate:edit')">
                    <template #title>编辑</template>
                    <a @click="navigateTo(`/certificate/edit/${record.id}`)">
                      <edit-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 查看获得学员 -->
                  <a-tooltip v-if="hasPermission('certificate:users')">
                    <template #title>查看获得学员</template>
                    <a @click="navigateTo(`/certificate/users/${record.id}`)">
                      <team-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 查看关联项 -->
                  <a-tooltip v-if="hasPermission('certificate:relate')">
                    <template #title>查看关联项</template>
                    <a @click="showRelatedModal(record)">
                      <link-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 删除 -->
                  <a-tooltip v-if="hasPermission('certificate:delete')">
                    <template #title>删除</template>
                    <a-popconfirm
                      title="确定要删除此证书吗？"
                      ok-text="确定"
                      cancel-text="取消"
                      @confirm="handleDelete(record)"
                    >
                      <a class="danger-link"><delete-outlined /></a>
                    </a-popconfirm>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-layout-content>
    </a-layout>

    <!-- 删除确认对话框 -->
    <a-modal
      v-model:visible="deleteModalVisible"
      title="确认删除"
      okText="删除"
      cancelText="取消"
      :okButtonProps="{ danger: true }"
      @ok="confirmDelete"
    >
      <p>确定要删除所选证书吗？此操作无法撤销。</p>
    </a-modal>

    <!-- 批量删除确认弹窗 -->
    <a-modal v-model:visible="batchDeleteModalVisible" title="批量删除证书" @ok="handleBatchDeleteConfirm" :okText="'确认'" :cancelText="'取消'">
      <p>确定要删除所选证书吗？此操作无法撤销。</p>
    </a-modal>

    <!-- 可见范围弹窗 -->
    <a-modal v-model:visible="visibilityModalVisible" title="可见范围设置" width="800px" :footer="null" destroyOnClose>
      <visibility-setting-component
        v-if="visibilityModalVisible"
        :biz-id="currentCertificate.id"
        biz-type="certificate"
        title="可见范围设置"
        @success="handleVisibilitySuccess"
        @cancel="visibilityModalVisible = false"
      />
    </a-modal>
  </a-layout>

    <!-- 关联项目弹窗 -->
    <relate-component v-model:visible="relatedModalVisible" :biz-id="currentCertificate.id" biz-type="CERTIFICATE" />
</template>

<script>
import { ref, reactive, onMounted, computed, defineComponent } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  StopOutlined,
  TeamOutlined,
  AppstoreOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  LinkOutlined
} from '@ant-design/icons-vue';
import { getCertificateList, deleteCertificate, batchDeleteCertificates, publishCertificate, unpublishCertificate, updateCertificate } from '@/api/certificate';
import { hasPermission } from '@/utils/permission';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import VisibilitySettingComponent from '@/components/common/VisibilitySettingComponent.vue';
import VisibilitySettingPage from '@/components/common/VisibilitySettingPage.vue';
import CertificateRelateComponent from '@/components/certificate/CertificateRelateComponent.vue';
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue';
import RelateComponent from '@/components/common/RelateComponent.vue';
import UserSelect from '@/components/common/UserSelect.vue';

export default defineComponent({
  name: 'CertificateListPage',
  components: {
    HeaderComponent,
    SiderComponent,
    SearchOutlined,
    ReloadOutlined,
    PlusOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    StopOutlined,
    TeamOutlined,
    AppstoreOutlined,
    VerticalAlignTopOutlined,
    VerticalAlignBottomOutlined,
    VisibilitySettingComponent,
    VisibilitySettingPage,
    PageBreadcrumb,
    LinkOutlined,
    CertificateRelateComponent,
    RelateComponent,
    UserSelect
  },
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['certificate']);
    const openKeys = ref(['operation']);
    const categoryModalVisible = ref(false);

    // 删除相关
    const deleteModalVisible = ref(false);
    const certificateToDelete = ref(null);
    const selectedRowKeys = ref([]);

    // 当前选中的证书
    const currentCertificate = ref({});

    // 可见范围弹窗
    const visibilityModalVisible = ref(false);

    // 分类设置弹窗
    const categorySettingModalVisible = ref(false);

    // 关联项目弹窗
    const relatedModalVisible = ref(false);

    // 表格数据
    const certificateList = ref([]);
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 搜索表单
    const searchForm = reactive({
      name: '',
      creatorId: undefined,
      status: undefined,
      startTime: '',
      endTime: '',
      onlyMine: false
    });
    const dateRange = ref([]);

    // 表格列定义
    const columns = [
      {
        title: '证书模板',
        key: 'templateUrl',
        fixed: 'left',
        width: 80
      },
      {
        title: '证书名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true,
        fixed: 'left',
        width: 160,
        sorter: true
      },
      {
        title: '描述',
        dataIndex: 'description',
        key: 'description',
        ellipsis: true
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 120
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        sorter: true,
        width: 180
      },
      // {
      //   title: '状态',
      //   dataIndex: 'status',
      //   key: 'status',
      //   width: 100
      // },
      {
        title: '操作',
        key: 'action',
        width: 200,
        fixed: 'right'
      }
    ];

    // 用户信息
    const userInfo = reactive({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });

    // 获取证书列表
    const fetchCertificateList = async () => {
      loading.value = true;
      try {
        const params = {
          ...searchForm,
          sortOrder: searchForm.sortOrder || undefined,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        };

        const res = await getCertificateList(params);
        if (res.code === 200) {
          certificateList.value = res.data.list.map(item => ({
            ...item,
            key: item.id
          }));
          pagination.total = res.data.total;
        } else {
          message.error(res.message || '获取证书列表失败');
        }
      } catch (error) {
        console.error('获取证书列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 表格选择变化
    const onSelectChange = (keys) => {
      selectedRowKeys.value = keys;
    };

    // 处理表格变化
    const handleTableChange = (pag, filters, sorter) => {
      pagination.current = pag.current;
      pagination.pageSize = pag.pageSize;

      // 处理排序
      if (sorter.field) {
        searchForm.sortField = sorter.field;
        searchForm.sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
      } else {
        searchForm.sortField = '';
        searchForm.sortOrder = '';
      }

      fetchCertificateList();
    };

    // 处理日期范围变化
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        searchForm.startTime = dates[0] ? dates[0].format('YYYY-MM-DD 00:00:00') : '';
        searchForm.endTime = dates[1] ? dates[1].format('YYYY-MM-DD 23:59:59') : '';
      } else {
        searchForm.startTime = '';
        searchForm.endTime = '';
      }
    };

    // 处理搜索
    const handleSearch = () => {
      pagination.current = 1;
      fetchCertificateList();
    };

    // 处理重置
    const handleReset = () => {
      // 重置搜索表单
      searchForm.name = '';
      searchForm.creatorId = undefined;
      searchForm.status = undefined;
      searchForm.startTime = '';
      searchForm.endTime = '';
      searchForm.onlyMine = false;
      dateRange.value = [];

      // 重置分页和排序
      pagination.current = 1;
      searchForm.sortField = '';
      searchForm.sortOrder = '';

      // 重新加载数据
      fetchCertificateList();
    };

    // 处理批量删除
    const handleBatchDelete = () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请至少选择一条记录');
        return;
      }

      deleteModalVisible.value = true;
    };

    // 确认删除
    const confirmDelete = async () => {
      try {
        const res = await batchDeleteCertificates(selectedRowKeys.value);
        if (res.code === 200) {
          message.success('删除成功');
          // 清空选择
          selectedRowKeys.value = [];
          // 重新加载数据
          fetchCertificateList();
          // 关闭弹窗
          deleteModalVisible.value = false;
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除失败:', error);
      }
    };

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      categoryModalVisible.value = true;
    };

    // 处理发布/取消发布
    const handlePublishToggle = async (record) => {
      try {
        let res;
        if (record.status === 'published') {
          // 禁用证书
          res = await unpublishCertificate(record.id);
          if (res.code === 200) {
            message.success('禁用成功');
          } else {
            message.error(res.message || '禁用失败');
          }
        } else {
          // 启用证书
          res = await publishCertificate(record.id);
          if (res.code === 200) {
            message.success('启用成功');
          } else {
            message.error(res.message || '启用失败');
          }
        }

        // 重新获取数据
        fetchCertificateList();
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    // 处理置顶/取消置顶
    const handleTopToggle = async (record) => {
      try {
        const res = await updateCertificate({
          id: record.id,
          isTop: record.isTop ? 0 : 1
        });

        if (res.code === 200) {
          message.success(record.isTop ? '取消置顶成功' : '置顶成功');
          // 重新获取数据
          fetchCertificateList();
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    // 显示可见范围弹窗
    const showVisibilityModal = (record) => {
      currentCertificate.value = record;
      visibilityModalVisible.value = true;
    };

    // 可见范围设置成功
    const handleVisibilitySuccess = () => {
      visibilityModalVisible.value = false;
      message.success('可见范围设置成功');
      fetchCertificateList();
    };

    // 显示分类设置弹窗
    const showCategorySettingModal = (record) => {
      currentCertificate.value = record;
      categorySettingModalVisible.value = true;
    };

    // 处理分类设置成功
    const handleCategorySettingSuccess = () => {
      categorySettingModalVisible.value = false;
      // 重新获取数据
      fetchCertificateList();
    };

    // 显示关联项目弹窗
    const showRelatedModal = (record) => {
      currentCertificate.value = record;
      relatedModalVisible.value = true;
    };

    // 处理删除
    const handleDelete = async (record) => {
      try {
        const res = await deleteCertificate(record.id);
        if (res.code === 200) {
          message.success('删除成功');
          // 重新获取数据
          fetchCertificateList();
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除失败:', error);
      }
    };

    // 页面跳转
    const navigateTo = (path) => {
      router.push(path);
    };

    onMounted(() => {
      // 获取证书列表
      fetchCertificateList();
    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      loading,
      certificateList,
      pagination,
      columns,
      searchForm,
      dateRange,
      categoryModalVisible,
      deleteModalVisible,
      certificateToDelete,
      selectedRowKeys,
      userInfo,
      currentCertificate,
      visibilityModalVisible,
      categorySettingModalVisible,
      relatedModalVisible,
      hasPermission,
      onSelectChange,
      handleTableChange,
      handleDateRangeChange,
      handleSearch,
      handleReset,
      handleBatchDelete,
      confirmDelete,
      showCategoryModal,
      navigateTo,
      handlePublishToggle,
      handleTopToggle,
      showVisibilityModal,
      handleVisibilitySuccess,
      showCategorySettingModal,
      handleCategorySettingSuccess,
      handleDelete,
      showRelatedModal
    };
  }
});
</script>

<style scoped>
.certificate-list-layout {
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

.search-card {
  margin-bottom: 16px;
}

.table-card {
  background-color: #fff;
}

.table-operations {
  margin-bottom: 16px;
}

.danger-link {
  color: #ff4d4f;
}

.danger-link:hover {
  color: #ff7875;
}
</style>
