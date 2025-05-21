<template><a-layout class="train-list-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" /><a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <PageBreadcrumb />
          <div class="content-header-right">
            <a-button v-if="hasPermission('train:create')" type="primary" @click="navigateTo('/train/create')">
              <plus-outlined />创建培训
            </a-button>
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <a-card class="search-card">
          <a-form layout="inline" :model="searchForm" :label-col="{ style: { width: '80px' } }"
            :wrapperCol="{ style: { flex: 1 } }">
            <a-row :gutter="16" style="width: 100%">
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="培训名称">
                  <a-input v-model:value="searchForm.title" placeholder="请输入培训名称" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="发布状态">
                  <a-select v-model:value="searchForm.status" placeholder="请选择发布状态" allowClear>
                    <a-select-option value="published">已发布</a-select-option>
                    <a-select-option value="draft">草稿</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="分类">
                  <CategoryTreeSelect multiple v-model:value="searchForm.categoryIds" />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建人">
                  <UserSelect v-model:value="searchForm.creatorId" :show-search="true" placeholder="请选择创建人" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建时间">
                  <a-range-picker v-model:value="dateRange" style="width: 100%" @change="handleDateRangeChange" />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 24 }" :xxl="{ span: 18 }"
                style="display: flex;justify-content: flex-end">
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
              <a-button v-if="hasPermission('train:batchDelete')" type="danger" :disabled="selectedRowKeys.length === 0"
                @click="handleBatchDelete">
                <delete-outlined />批量删除
              </a-button>
              <a-button v-if="hasPermission('train:batchChangeCreator')" type="primary" :disabled="selectedRowKeys.length === 0"
                @click="showChangeCreatorModal">
                <user-switch-outlined />批量变更创建人
              </a-button>
            </a-space>
          </div>

          <!-- 培训列表表格 -->
          <a-table :columns="columns" :data-source="trainList" :pagination="pagination" :loading="loading"
            :row-selection="{ selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id"
            :scroll="{ x: '100%' }" @change="handleTableChange">
            <!-- 封面图列 -->
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'cover'">
                <a-image :width="40" :src="record.cover || 'https://via.placeholder.com/40'" />
              </template>

              <!-- 培训名称列 -->
              <template v-if="column.key === 'name'">
                <a @click="navigateTo(`/train/detail/${record.id}`)">{{ record.title }}</a>
              </template>

              <!-- 分类列 -->
              <template v-if="column.key === 'categoryNames'">
                <template v-if="record.categoryNames && record.categoryNames.length">
                  <a-tag v-for="(category, index) in record.categoryNames" :key="index" color="blue">
                    {{ category }}
                  </a-tag>
                </template>
                <span v-else>-</span>
              </template>

              <!-- 状态列 -->
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'published' ? 'green' : 'orange'">
                  {{ record.status === 'published' ? '已发布' : '草稿' }}
                </a-tag>
              </template>

              <!-- 操作列 -->
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <!-- 编辑 -->
                  <a-tooltip v-if="hasPermission('train:edit')">
                    <template #title>编辑</template>
                    <a @click="navigateTo(`/train/edit/${record.id}`)">
                      <edit-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 指派 -->
                  <a-tooltip v-if="hasPermission('train:assign')">
                    <template #title>指派</template>
                    <a @click="showAssignModal(record)">
                      <user-add-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 跟踪 -->
                  <a-tooltip v-if="hasPermission('train:track')">
                    <template #title>跟踪</template>
                    <a @click="navigateTo(`/train/tracking/${record.id}`)">
                      <line-chart-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 分类 -->
                  <a-tooltip v-if="hasPermission('train:category')">
                    <template #title>分类</template>
                    <a @click="showCategorySettingModal(record)">
                      <appstore-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 指派记录 -->
                  <a-tooltip v-if="hasPermission('train:assignRecord')">
                    <template #title>指派记录</template>
                    <a @click="showAssignRecordModal(record)">
                      <unordered-list-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 可见范围 -->
                  <a-tooltip v-if="hasPermission('train:visibility')">
                    <template #title>可见范围</template>
                    <a @click="showVisibilityModal(record)">
                      <eye-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 置顶/取消置顶 -->
                  <!-- <a-tooltip v-if="hasPermission('train:top')">
                    <template #title>{{ record.isTop ? '取消置顶' : '置顶' }}</template>
                    <a @click="handleTopToggle(record)">
                      <vertical-align-top-outlined v-if="!record.isTop" />
                      <vertical-align-bottom-outlined v-else />
                    </a>
                  </a-tooltip> -->

                  <!-- 分享 -->
                  <a-tooltip v-if="hasPermission('train:share')">
                    <template #title>分享</template>
                    <a @click="handleShare(record)">
                      <share-alt-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 日志 -->
                  <a-tooltip v-if="hasPermission('train:log')">
                    <template #title>日志</template>
                    <a @click="showLogModal(record)">
                      <history-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 查看关联项 -->
                  <a-tooltip v-if="hasPermission('train:related')">
                    <template #title>查看关联项</template>
                    <a @click="showRelatedModal(record)">
                      <link-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 发布/取消发布 -->
                  <a-tooltip v-if="hasPermission('train:publish')">
                    <template #title>{{ record.status === 'published' ? '取消发布' : '发布' }}</template>
                    <a @click="handlePublishToggle(record)">
                      <check-circle-outlined v-if="record.status !== 'published'" />
                      <stop-outlined v-else />
                    </a>
                  </a-tooltip>

                  <!-- 删除 -->
                  <a-tooltip v-if="hasPermission('train:delete')">
                    <template #title>删除</template>
                    <a-popconfirm title="确定要删除此培训吗？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
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

    <!-- 可见范围设置弹窗 -->
    <a-modal v-model:visible="visibilityModalVisible" title="可见范围设置" width="800px" :footer="null" destroyOnClose>
      <visibility-setting-component v-if="visibilityModalVisible" :biz-id="currentTrain.id" business-type="TRAIN"
        @success="handleVisibilitySuccess" @cancel="visibilityModalVisible = false" :selectedRange="selectedRange" />
    </a-modal>

    <!-- 分类设置弹窗 -->
    <a-modal v-model:visible="categorySettingModalVisible" title="培训分类设置" width="600px" :footer="null" destroyOnClose>
      <category-setting-component v-if="categorySettingModalVisible" :biz-id="currentTrain.id" bizType="TRAIN"
        :current-categories="currentTrain.categoryIds?.split(',') || []" @success="handleCategorySettingSuccess"
        @cancel="categorySettingModalVisible = false" />
    </a-modal>

    <!-- 指派弹窗 -->
    <a-modal v-model:visible="assignModalVisible" title="培训指派" width="800px" :footer="null" destroyOnClose>
      <assign-page v-if="assignModalVisible" :visible="assignModalVisible" :biz-id="currentTrain.id" business-type="TRAIN"
        @success="handleAssignSuccess" @cancel="assignModalVisible = false" />
    </a-modal>

    <!-- 指派记录弹窗 -->
    <AssignRecordsModalComponent
      v-model:visible="assignRecordModalVisible"
      :type="'TRAIN'"
      :typeId="currentTrain.id"
    />

    <!-- 日志弹窗 -->
    <a-modal v-model:visible="logModalVisible" title="操作日志" width="800px" :footer="null" destroyOnClose>
      <div v-if="logModalVisible" class="log-container">
        <a-table :columns="logColumns" :data-source="logList" :pagination="logPagination" :loading="logLoading"
          @change="handleLogTableChange">
        </a-table>
      </div>
    </a-modal>

    <!-- 关联项目弹窗 -->
    <relate-component v-model:visible="relatedModalVisible" :biz-id="currentTrain.id" biz-type="TRAIN"
      :showFilter="false" />

    <!-- 批量变更创建人弹窗 -->
    <a-modal v-model:visible="changeCreatorModalVisible" title="批量变更创建人" @ok="handleChangeCreator"
      @cancel="changeCreatorModalVisible = false" :confirmLoading="changeCreatorLoading">
      <a-form :model="changeCreatorForm" layout="vertical">
        <a-form-item label="新创建人" name="newCreatorId" :rules="[{ required: true, message: '请选择新创建人' }]">
          <UserSelect v-model:value="changeCreatorForm.newCreatorId" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  getTrainList,
  deleteTrain,
  batchDeleteTrain,
  publishTrain,
  unpublishTrain,
  updateTrain,
  getTrainLearners,
  batchUpdateCreator
} from '@/api/train';
import { getCategoryList } from '@/api/category';
import { getOperationLogs } from '@/api/operation';
import { hasPermission } from '@/utils/permission';
import VisibilitySettingComponent from '@/components/common/VisibilitySettingComponent.vue';
import CategorySettingComponent from '@/components/common/CategorySettingComponent.vue';
import AssignPage from '@/components/train/AssignPage.vue';
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
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  StopOutlined,
  LinkOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  UserAddOutlined,
  LineChartOutlined,
  UnorderedListOutlined,
  ShareAltOutlined,
  HistoryOutlined,
  FileTextOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons-vue';
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue';
import RelateComponent from '@/components/common/RelateComponent.vue';
import AssignRecordsModalComponent from '@/components/common/AssignRecordsModalComponent.vue';
import UserSelect from '@/components/common/UserSelect.vue';
import { queryRange } from '@/api/common';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';

export default defineComponent({
  name: 'TrainListPage',
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
    PlusOutlined,
    SearchOutlined,
    ReloadOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    StopOutlined,
    LinkOutlined,
    VerticalAlignTopOutlined,
    VerticalAlignBottomOutlined,
    UserAddOutlined,
    LineChartOutlined,
    UnorderedListOutlined,
    ShareAltOutlined,
    HistoryOutlined,
    FileTextOutlined,
    VisibilitySettingComponent,
    CategorySettingComponent,
    AssignPage,
    PageBreadcrumb,
    RelateComponent,
    AssignRecordsModalComponent,
    UserSelect,
    CategoryTreeSelect,
    UserSwitchOutlined,
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const collapsed = ref(false);
    const selectedKeys = ref(['trainList']);
    const openKeys = ref(['learning']);
    const categoryModalVisible = ref(false);

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

    // 权限列表


    // 培训列表数据
    const trainList = ref([]);
    const loading = ref(false);
    const selectedRowKeys = ref([]);

    // 分类列表
    const categories = ref([]);

    // 日期范围
    const dateRange = ref([]);

    // 搜索表单
    const searchForm = reactive({
      title: '',
      status: undefined,
      categoryIds: [],
      creatorId: undefined,
      startTime: '',
      endTime: '',
      onlyMine: false,
      pageNum: 1,
      pageSize: 10,
      sortField: 'gmtCreate',
      sortOrder: 'desc'
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
        title: 'id',
        dataIndex: 'id',
        key: 'id',
        fixed: 'left',
        width: 60
      },
      {
        title: '封面',
        dataIndex: 'cover',
        key: 'cover',
        fixed: 'left',
        width: 60
      },
      {
        title: '培训名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true,
        fixed: 'left',
        width: 200
      },
      {
        title: '分类',
        dataIndex: 'categoryNames',
        key: 'categoryNames',
        width: 150
      },
      {
        title: '学分',
        dataIndex: 'credit',
        key: 'credit',
        width: 60
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 100
      },
      {
        title: '更新时间',
        dataIndex: 'gmtModified',
        key: 'gmtModified',
        width: 150,
        sorter: true
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 120
      }
    ];

    // 当前选中的培训
    const currentTrain = ref({});

    // 可见范围弹窗
    const visibilityModalVisible = ref(false);

    // 分类设置弹窗
    const categorySettingModalVisible = ref(false);

    // 指派弹窗
    const assignModalVisible = ref(false);

    // 指派记录弹窗
    const assignRecordModalVisible = ref(false);

    // 日志弹窗
    const logModalVisible = ref(false);
    const logLoading = ref(false);
    const logList = ref([]);
    const logPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });
    const logColumns = [
      {
        title: '操作时间',
        dataIndex: 'time',
        width: 180,
        key: 'time'
      },
      {
        title: '操作人',
        dataIndex: 'operator',
        key: 'operator'
      },
      {
        title: '操作类型',
        dataIndex: 'operateType',
        key: 'operateType'
      },
      {
        title: '操作内容',
        dataIndex: 'message',
        key: 'message',
        width: 360,
        ellipsis: true
      }
    ];

    // 可见范围
    const selectedRange = ref([]);

    // 关联项目弹窗
    const relatedModalVisible = ref(false);
    const relatedActiveKey = ref('courses');

    // 关联证书
    const relatedCertificatesLoading = ref(false);
    const relatedCertificatesList = ref([]);
    const relatedCertificatesPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });
    const relatedCertificatesColumns = [
      {
        title: '证书名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true
      },
      {
        title: '发放条件',
        dataIndex: 'issueCondition',
        key: 'issueCondition',
        ellipsis: true
      },
      {
        title: '有效期',
        dataIndex: 'validPeriod',
        key: 'validPeriod',
        customRender: ({ text }) => text ? `${text}个月` : '永久'
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName'
      },
      {
        title: '操作',
        key: 'action',
        width: 80
      }
    ];

    // 批量变更创建人弹窗
    const changeCreatorModalVisible = ref(false);
    const changeCreatorLoading = ref(false);
    const changeCreatorForm = reactive({
      newCreatorId: undefined
    });

    // 获取培训列表
    const fetchTrainList = async () => {
      loading.value = true;
      try {
        const res = await getTrainList(searchForm);
        if (res.code === 200 && res.data) {
          trainList.value = res.data.list || [];
          pagination.total = res.data.total || 0;
          pagination.current = searchForm.pageNum;
          pagination.pageSize = searchForm.pageSize;
        } else {
          message.error(res.message || '获取培训列表失败');
        }
      } catch (error) {
        console.error('获取培训列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          categories.value = res.data || [];
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      }
    };

    // 处理日期范围变更
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        searchForm.startTime = dates[0].format('YYYY-MM-DD');
        searchForm.endTime = dates[1].format('YYYY-MM-DD');
      } else {
        searchForm.startTime = '';
        searchForm.endTime = '';
      }
    };

    // 处理搜索
    const handleSearch = () => {
      searchForm.pageNum = 1;
      fetchTrainList();
    };

    // 处理重置
    const handleReset = () => {
      // 重置搜索表单
      searchForm.title = '';
      searchForm.status = undefined;
      searchForm.categoryIds = [];
      searchForm.creatorId = undefined;
      searchForm.startTime = '';
      searchForm.endTime = '';
      searchForm.onlyMine = false;
      searchForm.pageNum = 1;
      searchForm.sortField = 'gmtCreate';
      searchForm.sortOrder = 'desc';

      // 重置日期范围
      dateRange.value = [];

      // 重新获取数据
      fetchTrainList();
    };

    // 处理表格变更
    const handleTableChange = (pag, filters, sorter) => {
      // 处理分页
      searchForm.pageNum = pag.current;
      searchForm.pageSize = pag.pageSize;

      // 处理排序
      if (sorter && sorter.field) {
        searchForm.sortField = sorter.field;
        searchForm.sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
      }

      // 重新获取数据
      fetchTrainList();
    };

    // 处理选择变更
    const onSelectChange = (keys) => {
      selectedRowKeys.value = keys;
    };

    // 处理删除培训
    const handleDelete = async (record) => {
      try {
        const res = await deleteTrain(record.id);
        if (res.code === 200) {
          message.success('删除成功');
          // 重新获取数据
          fetchTrainList();
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除培训失败:', error);
      }
    };

    // 处理批量删除
    const handleBatchDelete = async () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要删除的培训');
        return;
      }

      try {
        const res = await batchDeleteTrain(selectedRowKeys.value);
        if (res.code === 200) {
          message.success(`成功删除 ${selectedRowKeys.value.length} 个培训`);

          // 清空选择
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchTrainList();
        } else {
          message.error(res.message || '批量删除失败');
        }
      } catch (error) {
        console.error('批量删除培训失败:', error);
      }
    };

    // 处理发布/取消发布
    const handlePublishToggle = async (record) => {
      try {
        let res;
        if (record.status === 'published') {
          // 取消发布
          res = await unpublishTrain(record.id);
          if (res.code === 200) {
            message.success('取消发布成功');
          } else {
            message.error(res.message || '取消发布失败');
          }
        } else {
          // 发布
          res = await publishTrain(record.id);
          if (res.code === 200) {
            message.success('发布成功');
          } else {
            message.error(res.message || '发布失败');
          }
        }

        // 重新获取数据
        fetchTrainList();
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    // 处理置顶/取消置顶
    const handleTopToggle = async (record) => {
      try {
        const res = await updateTrain({
          id: record.id,
          isTop: record.isTop ? 0 : 1
        });

        if (res.code === 200) {
          message.success(record.isTop ? '取消置顶成功' : '置顶成功');
          // 重新获取数据
          fetchTrainList();
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    /** 获取范围回显 */
    const getRange = (id) => {
      queryRange({
        businessType: 'TRAIN',
        businessId: id,
        functionType: 'visibility'
      }).then(res => {
        console.log(res, '范围配置')

        const tempArray = [];

        const { departmentInfos, roleInfos, userInfos } = res.data;
        departmentInfos.forEach(item => {
          tempArray.push({ id: item.departmentId, type: 'department', name: item.departmentName });
        })
        roleInfos.forEach(item => {
          tempArray.push({ id: item.roleId, type: 'role', name: item.roleName });
        })
        userInfos.forEach(item => {
          tempArray.push({ id: item.userId, type: 'user', name: item.userName });
        })

        selectedRange.value = tempArray;
      });
    };

    // 显示可见范围弹窗
    const showVisibilityModal = (record) => {
      currentTrain.value = record;
      getRange(record.id)
      visibilityModalVisible.value = true;
    };

    // 处理可见范围设置成功
    const handleVisibilitySuccess = () => {
      visibilityModalVisible.value = false;
    };

    // 显示分类设置弹窗
    const showCategorySettingModal = (record) => {
      currentTrain.value = record;
      categorySettingModalVisible.value = true;
    };

    // 处理分类设置成功
    const handleCategorySettingSuccess = () => {
      categorySettingModalVisible.value = false;
      // 重新获取数据
      fetchTrainList();
    };

    // 显示指派弹窗
    const showAssignModal = (record) => {
      console.log(record)
      currentTrain.value = record;
      assignModalVisible.value = true;
    };

    // 处理指派成功
    const handleAssignSuccess = () => {
      assignModalVisible.value = false;
      message.success('指派成功');
    };

    // 显示指派记录弹窗
    const showAssignRecordModal = (record) => {
      currentTrain.value = record;
      assignRecordModalVisible.value = true;
    };

    // 显示日志弹窗
    const showLogModal = (record) => {
      currentTrain.value = record;
      logModalVisible.value = true;
      // 获取日志数据
      fetchLogs();
    };

    // 获取日志数据
    const fetchLogs = async () => {
      if (!currentTrain.value.id) return;

      logLoading.value = true;
      try {
        const res = await getOperationLogs({
          businessType: 'TRAIN',
          businessId: currentTrain.value.id,
          pageNum: logPagination.current,
          pageSize: logPagination.pageSize
        });

        if (res.code === 200 && res.data) {
          logList.value = res.data.logs || [];
          logPagination.total = res.data.total || 0;
        } else {
          message.error(res.message || '获取日志失败');
        }
      } catch (error) {
        console.error('获取日志失败:', error);
      } finally {
        logLoading.value = false;
      }
    };

    // 处理日志表格变更
    const handleLogTableChange = (pag) => {
      logPagination.current = pag.current;
      logPagination.pageSize = pag.pageSize;
      fetchLogs();
    };

    // 显示关联项目弹窗
    const showRelatedModal = (record) => {
      currentTrain.value = record;
      relatedModalVisible.value = true;
      relatedActiveKey.value = 'courses';
    };

    // 获取关联证书数据
    const fetchRelatedCertificates = async () => {
      if (!currentTrain.value.id) return;

      relatedCertificatesLoading.value = true;
      try {
        // 模拟数据，实际项目中应该调用API获取数据
        setTimeout(() => {
          relatedCertificatesList.value = [
            {
              id: 1,
              name: 'JavaScript开发证书',
              issueCondition: '完成所有课程并通过考试',
              validPeriod: 12,
              creatorName: '管理员'
            }
          ];
          relatedCertificatesPagination.total = 1;
          relatedCertificatesLoading.value = false;
        }, 500);
      } catch (error) {
        console.error('获取关联证书失败:', error);
        relatedCertificatesLoading.value = false;
      }
    };

    // 处理关联证书表格变更
    const handleRelatedCertificatesTableChange = (pag) => {
      relatedCertificatesPagination.current = pag.current;
      relatedCertificatesPagination.pageSize = pag.pageSize;
      fetchRelatedCertificates();
    };

    // 处理分享
    const handleShare = (record) => {
      // 生成分享链接
      const shareLink = `${window.location.origin}/train/detail/${record.id}?share=true`;

      // 复制到剪贴板
      navigator.clipboard.writeText(shareLink)
        .then(() => {
          message.success('分享链接已复制到剪贴板');
        })
        .catch(() => {
          message.error('复制失败，请手动复制');
          // 显示分享链接
          message.info(shareLink);
        });
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

    // 监听relatedActiveKey变化
    watch(() => relatedActiveKey.value, (newVal) => {
      if (newVal === 'certificates') {
        fetchRelatedCertificates();
      } else if (newVal === 'courses') {
        fetchRelatedCourses();
      }
    });

    // 显示批量变更创建人弹窗
    const showChangeCreatorModal = () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要操作的培训');
        return;
      }

      changeCreatorForm.newCreatorId = undefined;
      changeCreatorModalVisible.value = true;
    };

    // 处理批量变更创建人
    const handleChangeCreator = async () => {
      if (!changeCreatorForm.newCreatorId) {
        message.warning('请选择新创建人');
        return;
      }

      changeCreatorLoading.value = true;
      try {
        const res = await batchUpdateCreator({
          courseIds: selectedRowKeys.value,
          newCreatorId: changeCreatorForm.newCreatorId
        });

        if (res.code === 200) {
          message.success('批量变更创建人成功');
          changeCreatorModalVisible.value = false;

          // 清空选择
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchTrainList();
        } else {
          message.error(res.message || '批量变更创建人失败');
        }
      } catch (error) {
        console.error('批量变更创建人失败:', error);
      } finally {
        changeCreatorLoading.value = false;
      }
    };

    onMounted(() => {
      // 获取分类列表
      fetchCategories();

      // 获取培训列表
      fetchTrainList();
    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      trainList,
      loading,
      selectedRowKeys,
      categories,
      dateRange,
      searchForm,
      pagination,
      columns,
      currentTrain,
      visibilityModalVisible,
      categorySettingModalVisible,
      assignModalVisible,
      assignRecordModalVisible,
      logModalVisible,
      logLoading,
      logList,
      logPagination,
      logColumns,
      relatedModalVisible,
      relatedActiveKey,
      relatedCertificatesLoading,
      relatedCertificatesList,
      relatedCertificatesPagination,
      relatedCertificatesColumns,
      categoryModalVisible,
      selectedRange,

      hasPermission,
      handleDateRangeChange,
      handleSearch,
      handleReset,
      handleTableChange,
      onSelectChange,
      handleDelete,
      handleBatchDelete,
      handlePublishToggle,
      handleTopToggle,
      showVisibilityModal,
      handleVisibilitySuccess,
      showCategorySettingModal,
      handleCategorySettingSuccess,
      showAssignModal,
      handleAssignSuccess,
      showAssignRecordModal,
      showLogModal,
      handleLogTableChange,
      showRelatedModal,
      handleRelatedCertificatesTableChange,
      handleShare,
      handleLogout,
      navigateTo,
      showCategoryModal,
      changeCreatorModalVisible,
      changeCreatorLoading,
      changeCreatorForm,
      showChangeCreatorModal,
      handleChangeCreator
    };
  }
});
</script>

<style scoped>
.train-list-layout {
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

.assign-record-container,
.log-container,
.related-container {
  padding: 16px;
}
</style>
